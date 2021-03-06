/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://boofcv.org).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package boofcv.alg.distort;

import boofcv.alg.geo.PerspectiveOps;
import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.alg.interpolate.TypeInterpolate;
import boofcv.core.image.border.BorderType;
import boofcv.core.image.border.FactoryImageBorder;
import boofcv.core.image.border.ImageBorder;
import boofcv.factory.distort.FactoryDistort;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.calib.IntrinsicParameters;
import boofcv.struct.distort.PixelTransform_F32;
import boofcv.struct.distort.PointTransform_F32;
import boofcv.struct.distort.SequencePointTransform_F32;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import georegression.struct.shapes.RectangleLength2D_F32;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;

/**
 * Operations related to manipulating lens distortion in images
 *
 * @author Peter Abeles
 */
public class LensDistortionOps {

	/**
	 * <p>
	 * Creates an {@link ImageDistort} class which will remove the lens distortion.  The user
	 * can select how the view is adjusted.
	 * </p>
	 *
	 * <p>
	 * If BorderType.VALUE then pixels outside the image will be filled in with a
	 * value of 0.  For viewing purposes it is recommended that BorderType.VALUE be used and BorderType.EXTENDED
	 * in computer vision applications.  VALUE creates harsh edges which can cause false positives
	 * when detecting features, which EXTENDED minimizes.
	 * </p>
	 *
	 * @param type The type of adjustment it will do
	 * @param borderType Specifies how the image border is handled. Null means borders are ignored.
	 * @param param Original intrinsic parameters.
	 * @param paramAdj (output) Intrinsic parameters which reflect the undistorted image.  Can be null.
	 * @param imageType Type of image it will undistort
	 * @return ImageDistort which removes lens distortion
	 */
	public static <T extends ImageBase>
	ImageDistort<T,T> removeDistortion( AdjustmentType type , BorderType borderType ,
										IntrinsicParameters param, IntrinsicParameters paramAdj ,
										ImageType<T> imageType )
	{
		Class bandType = imageType.getImageClass();

		InterpolatePixelS interp =
				FactoryInterpolation.createPixelS(0, 255, TypeInterpolate.BILINEAR, bandType);

		ImageBorder border;
		if( borderType == null ) {
			border = null;
		} else if( borderType == BorderType.VALUE )
			border = FactoryImageBorder.value(bandType, 0);
		else
			border = FactoryImageBorder.general(bandType,borderType);

		PointTransform_F32 undistToDist = null;
		switch( type ) {
			case ALL_INSIDE:
				undistToDist = allInside(param, paramAdj,true);
				break;

			case FULL_VIEW:
				undistToDist = fullView(param,paramAdj,true);
				break;

			case NONE:
				undistToDist = distortTransform(param).distort_F32(true, true);
				break;
		}

		ImageDistort<T,T> distort;

		switch( imageType.getFamily() ) {
			case SINGLE_BAND:
				distort = FactoryDistort.distort(true,interp, border, bandType);
				break;

			case MULTI_SPECTRAL:
				distort = FactoryDistort.distortMS(true,interp, border, bandType);
				break;

			default:
				throw new RuntimeException("Unsupported image family: "+imageType.getFamily());
		}

		distort.setModel(new PointToPixelTransform_F32(undistToDist));

		return distort;
	}

	/**
	 * <p>
	 * Transforms the view such that the entire original image is visible after lens distortion has been removed.
	 * The appropriate {@link PointTransform_F32} is returned and a new set of intrinsic camera parameters for
	 * the "virtual" camera that is associated with the returned transformed.
	 * </p>
	 *
	 * @param param Intrinsic camera parameters.
	 * @param paramAdj If not null, the new camera parameters for the undistorted view are stored here.
	 * @param adjToDistorted If true then the transform's input is assumed to be pixels in the adjusted undistorted
	 *                       image and the output will be in distorted image, if false then the reverse transform
	 *                       is returned.
	 * @return The requested transform
	 */
	public static PointTransform_F32 fullView( IntrinsicParameters param,
											   IntrinsicParameters paramAdj ,
											   boolean adjToDistorted ) {

		PointTransform_F32 remove_p_to_p = distortTransform(param).undistort_F32(true, true);

		RectangleLength2D_F32 bound = DistortImageOps.boundBox_F32(param.width, param.height,
				new PointToPixelTransform_F32(remove_p_to_p));

		double scaleX = bound.width/param.width;
		double scaleY = bound.height/param.height;

		double scale = Math.max(scaleX, scaleY);

		// translation
		double deltaX = bound.x0;
		double deltaY = bound.y0;

		// adjustment matrix
		DenseMatrix64F A = new DenseMatrix64F(3,3,true,scale,0,deltaX,0,scale,deltaY,0,0,1);

		return adjustmentTransform(param, paramAdj, adjToDistorted, remove_p_to_p, A);
	}

	/**
	 * <p>
	 * Adjusts the view such that each pixel has a correspondence to the original image while maximizing the
	 * view area. In other words no black regions which can cause problems for some image processing algorithms.
	 * </p>
	 *
	 * @param param Intrinsic camera parameters.
	 * @param paramAdj If not null, the new camera parameters for the undistorted view are stored here.
	 * @param adjToDistorted If true then the transform's input is assumed to be pixels in the adjusted undistorted
	 *                       image and the output will be in distorted image, if false then the reverse transform
	 *                       is returned.
	 * @return The requested transform
	 */
	public static PointTransform_F32 allInside( IntrinsicParameters param,
												IntrinsicParameters paramAdj ,
												boolean adjToDistorted ) {
		PointTransform_F32 remove_p_to_p = distortTransform(param).undistort_F32(true, true);

		RectangleLength2D_F32 bound = LensDistortionOps.boundBoxInside(param.width, param.height,
				new PointToPixelTransform_F32(remove_p_to_p));

		// ensure there are no strips of black
		LensDistortionOps.roundInside(bound);

		double scaleX = bound.width/param.width;
		double scaleY = bound.height/param.height;

		double scale = Math.min(scaleX, scaleY);

		// translation and shift over so that the small axis is in the middle
		double deltaX = bound.x0 + (scaleX-scale)*param.width/2.0;
		double deltaY = bound.y0 + (scaleY-scale)*param.height/2.0;

		// adjustment matrix
		DenseMatrix64F A = new DenseMatrix64F(3,3,true,scale,0,deltaX,0,scale,deltaY,0,0,1);

		return adjustmentTransform(param, paramAdj, adjToDistorted, remove_p_to_p, A);
	}

	/**
	 * Given the lens distortion and the intrinsic adjustment matrix compute the new intrinsic parameters
	 * and {@link PointTransform_F32}
	 */
	private static PointTransform_F32 adjustmentTransform(IntrinsicParameters param,
														  IntrinsicParameters paramAdj,
														  boolean adjToDistorted,
														  PointTransform_F32 remove_p_to_p,
														  DenseMatrix64F A) {
		DenseMatrix64F A_inv = null;

		if( !adjToDistorted || paramAdj != null ) {
			A_inv = new DenseMatrix64F(3, 3);
			if (!CommonOps.invert(A, A_inv)) {
				throw new RuntimeException("Failed to invert adjustment matrix.  Probably bad.");
			}
		}

		if( paramAdj != null ) {
			PerspectiveOps.adjustIntrinsic(param, A_inv, paramAdj);
		}

		if( adjToDistorted ) {
			PointTransform_F32 add_p_to_p = distortTransform(param).distort_F32(true, true);
			PointTransformHomography_F32 adjust = new PointTransformHomography_F32(A);

			return new SequencePointTransform_F32(adjust,add_p_to_p);
		} else {
			PointTransformHomography_F32 adjust = new PointTransformHomography_F32(A_inv);

			return new SequencePointTransform_F32(remove_p_to_p,adjust);
		}
	}

	/**
	 * Creates the {@link LensDistortionPinhole lens distortion} for the specified camera parameters.
	 * Call this to create transforms to and from pixel and normalized image coordinates with and without
	 * lens distortion.
	 */
	public static LensDistortionPinhole distortTransform(IntrinsicParameters param) {
		return new LensDistortionRadialTangential(param);
	}

	/**
	 * Finds the maximum area axis-aligned rectangle contained inside the transformed image which
	 * does not include any pixels outside the sources border.  Assumes that the coordinates are not
	 * flipped and some other stuff too.
	 *
	 * @param srcWidth Width of the source image
	 * @param srcHeight Height of the source image
	 * @param transform Transform being applied to the image
	 * @return Bounding box
	 */
	public static RectangleLength2D_F32 boundBoxInside(int srcWidth, int srcHeight,
												 PixelTransform_F32 transform) {

		float x0,y0,x1,y1;

		transform.compute(0,0);
		x0 = transform.distX;
		y0 = transform.distY;

		transform.compute(srcWidth,0);
		x1=transform.distX;
		transform.compute(0, srcHeight);
		y1=transform.distY;

		for( int x = 0; x < srcWidth; x++ ) {
			transform.compute(x, 0);
			if( transform.distY > y0 )
				y0 = transform.distY;
			transform.compute(x,srcHeight);
			if( transform.distY < y1 )
				y1 = transform.distY;
		}

		for( int y = 0; y < srcHeight; y++ ) {
			transform.compute(0,y);
			if( transform.distX > x0 )
				x0 = transform.distX;
			transform.compute(srcWidth,y);
			if( transform.distX < x1 )
				x1 = transform.distX;
		}

		return new RectangleLength2D_F32(x0,y0,x1-x0,y1-y0);
	}

	/**
	 * Adjust bound to ensure the entire image is contained inside, otherwise there might be
	 * single pixel wide black regions
	 */
	public static void roundInside( RectangleLength2D_F32 bound ) {
		float x0 = (float)Math.ceil(bound.x0);
		float y0 = (float)Math.ceil(bound.y0);
		float x1 = (float)Math.floor(bound.x0+bound.width);
		float y1 = (float)Math.floor(bound.y0+bound.height);

		bound.x0 = x0;
		bound.y0 = y0;
		bound.width = x1-x0;
		bound.height = y1-y0;
	}
}

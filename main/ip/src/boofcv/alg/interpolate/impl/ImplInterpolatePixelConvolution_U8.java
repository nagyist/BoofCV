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

package boofcv.alg.interpolate.impl;

import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.core.image.border.ImageBorder;
import boofcv.core.image.border.ImageBorder_I32;
import boofcv.struct.convolve.KernelContinuous1D_F32;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.ImageUInt8;

/**
 * <p>
 * Performs interpolation by convolving a continuous-discrete function across the image.  Borders are handled by
 * re-normalizing.  It is assumed that the kernel will sum up to one.  This is particularly
 * important for the unsafe_get() function which does not re-normalize.
 * </p>
 *
 * <p>
 * DO NOT MODIFY: Generated by {@link GenerateImplInterpolatePixelConvolution}.
 * </p>
 *
 * @author Peter Abeles
 */
public class ImplInterpolatePixelConvolution_U8 implements InterpolatePixelS<ImageUInt8>  {

	// used to read outside the image border
	private ImageBorder_I32 border;
	// kernel used to perform interpolation
	private KernelContinuous1D_F32 kernel;
	// input image
	private ImageUInt8 image;
	// minimum and maximum allowed pixel values
	private float min,max;

	public ImplInterpolatePixelConvolution_U8(KernelContinuous1D_F32 kernel , float min , float max ) {
		this.kernel = kernel;
		this.min = min;
		this.max = max;
	}

	@Override
	public void setBorder(ImageBorder<ImageUInt8> border) {
		this.border = (ImageBorder_I32)border;
	}

	@Override
	public void setImage(ImageUInt8 image ) {
		if( border != null )
			border.setImage(image);
		this.image = image;
	}

	@Override
	public ImageUInt8 getImage() {
		return image;
	}

	@Override
	public float get(float x, float y) {

		if( x < 0 || y < 0 || x > image.width-1 || y > image.height-1 )
			throw new IllegalArgumentException("Pixel out of bounds. "+x+" "+y);

		int xx = (int)x;
		int yy = (int)y;

		final int radius = kernel.getRadius();
		final int width = kernel.getWidth();

		int x0 = xx - radius;
		int x1 = x0 + width;

		int y0 = yy - radius;
		int y1 = y0 + width;

		if( x0 < 0 ) x0 = 0;
		if( x1 > image.width ) x1 = image.width;

		if( y0 < 0 ) y0 = 0;
		if( y1 > image.height ) y1 = image.height;

		float value = 0;
		float totalWeightY = 0;
		for( int i = y0; i < y1; i++ ) {
			int indexSrc = image.startIndex + i*image.stride + x0;
			float totalWeightX = 0;
			float valueX = 0;
			for( int j = x0; j < x1; j++ ) {
				float w = kernel.compute(j-x);
				totalWeightX += w;
				valueX += w * (image.data[ indexSrc++ ]& 0xFF);
			}
			float w = kernel.compute(i-y);
			totalWeightY +=  w;
			value += w*valueX/totalWeightX;
		}

		value /= totalWeightY;
		
		if( value > max )
			return max;
		else if( value < min )
			return min;
		else
			return value;
	}

	@Override
	public float get_border(float x, float y) {
		int xx = (int)Math.floor(x);
		int yy = (int)Math.floor(y);

		final int radius = kernel.getRadius();
		final int width = kernel.getWidth();

		int x0 = xx - radius;
		int x1 = x0 + width;

		int y0 = yy - radius;
		int y1 = y0 + width;

		float value = 0;
		for( int i = y0; i < y1; i++ ) {
			float valueX = 0;
			for( int j = x0; j < x1; j++ ) {
				float w = kernel.compute(j-x);
				valueX += w * border.get(j,i);
			}
			float w = kernel.compute(i-y);
			value += w*valueX;
		}

		if( value > max )
			return max;
		else if( value < min )
			return min;
		else
			return value;
	}

	@Override
	public float get_fast(float x, float y) {
		int xx = (int)x;
		int yy = (int)y;

		final int radius = kernel.getRadius();
		final int width = kernel.getWidth();

		int x0 = xx - radius;
		int x1 = x0 + width;

		int y0 = yy - radius;
		int y1 = y0 + width;

		float value = 0;
		for( int i = y0; i < y1; i++ ) {
			int indexSrc = image.startIndex + i*image.stride + x0;
			float valueX = 0;
			for( int j = x0; j < x1; j++ ) {
				float w = kernel.compute(j-x);
				valueX += w * (image.data[ indexSrc++ ]& 0xFF);
			}
			float w = kernel.compute(i-y);
			value += w*valueX;
		}

		if( value > max )
			return max;
		else if( value < min )
			return min;
		else
			return value;
	}
	@Override
	public boolean isInFastBounds(float x, float y) {
		float r = kernel.getRadius();
		
		return (x-r >= 0 && y-r >= 0 && x+r < image.width && y+r <image.height);
	}
	@Override
	public int getFastBorderX() {
		return kernel.getRadius();
	}

	@Override
	public int getFastBorderY() {
		return kernel.getRadius();
	}

	@Override
	public ImageType<ImageUInt8> getImageType() {
		return ImageType.single(ImageUInt8.class);
	}

}

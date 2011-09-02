/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package boofcv.factory.interpolate;

import boofcv.alg.interpolate.InterpolatePixel;
import boofcv.alg.interpolate.InterpolateRectangle;
import boofcv.alg.interpolate.TypeInterpolate;
import boofcv.alg.interpolate.impl.*;
import boofcv.alg.interpolate.kernel.BicubicKernel_F32;
import boofcv.struct.image.*;

/**
 * Simplified interface for creating interpolation classes.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"unchecked"})
public class FactoryInterpolation {

	public static <T extends ImageBase> InterpolatePixel<T>
	createPixel( Class<T> imageType , TypeInterpolate type )
	{
		switch( type ) {
			case NEAREST_NEIGHBOR:
				return nearestNeighborPixel(imageType);

			case BILINEAR:
				return bilinearPixel(imageType);

			case BICUBIC:
				return bicubic(imageType,0.5f);
		}
		throw new IllegalArgumentException("Add type: "+type);
	}

	public static <T extends ImageBase> InterpolatePixel<T> bilinearPixel( T image ) {

		InterpolatePixel<T> ret = bilinearPixel((Class)image.getClass());
		ret.setImage(image);

		return ret;
	}

	public static <T extends ImageBase> InterpolatePixel<T> bilinearPixel(Class<T> type ) {
		if( type == ImageFloat32.class )
			return (InterpolatePixel<T>)new BilinearPixel_F32();
		else if( type == ImageUInt8.class )
			return (InterpolatePixel<T>)new BilinearPixel_U8();
		else if( type == ImageSInt16.class )
			return (InterpolatePixel<T>)new BilinearPixel_S16();
		else if( type == ImageSInt32.class )
			return (InterpolatePixel<T>)new BilinearPixel_S32();
		else
			throw new RuntimeException("Unknown image type: "+type.getName());
	}

	public static <T extends ImageBase> InterpolateRectangle<T> bilinearRectangle( T image ) {

		InterpolateRectangle<T> ret = bilinearRectangle((Class)image.getClass());
		ret.setImage(image);

		return ret;
	}

	public static <T extends ImageBase> InterpolateRectangle<T> bilinearRectangle( Class<T> type ) {
		if( type == ImageFloat32.class )
			return (InterpolateRectangle<T>)new BilinearRectangle_F32();
		else if( type == ImageUInt8.class )
			return (InterpolateRectangle<T>)new BilinearRectangle_U8();
		else if( type == ImageSInt16.class )
			return (InterpolateRectangle<T>)new BilinearRectangle_S16();
		else
			throw new RuntimeException("Unknown image type: "+type.getName());
	}

	public static <T extends ImageBase> InterpolatePixel<T> nearestNeighborPixel( Class<T> type ) {
		if( type == ImageFloat32.class )
			return (InterpolatePixel<T>)new NearestNeighborPixel_F32();
		else if( type == ImageUInt8.class )
			return (InterpolatePixel<T>)new NearestNeighborPixel_U8();
		else if( type == ImageSInt16.class )
			return (InterpolatePixel<T>)new NearestNeighborPixel_S16();
		else if( type == ImageSInt32.class )
			return (InterpolatePixel<T>)new NearestNeighborPixel_S32();
		else
			throw new RuntimeException("Unknown image type: "+type.getName());
	}

	public static <T extends ImageBase> InterpolateRectangle<T> nearestNeighborRectangle( Class<?> type ) {
		if( type == ImageFloat32.class )
			return (InterpolateRectangle<T>)new NearestNeighborRectangle_F32();
//		else if( type == ImageUInt8.class )
//			return (InterpolateRectangle<T>)new NearestNeighborRectangle_U8();
//		else if( type == ImageSInt16.class )
//			return (InterpolateRectangle<T>)new NearestNeighborRectangle_S16();
		else
			throw new RuntimeException("Unknown image type: "+type.getName());
	}

	public static <T extends ImageBase> InterpolatePixel<T> bicubic(Class<T> type , float param ) {
		BicubicKernel_F32 kernel = new BicubicKernel_F32(param);
		if( type == ImageFloat32.class )
			return (InterpolatePixel<T>)new ImplInterpolatePixelConvolution_F32(kernel);
		else if( type == ImageUInt8.class )
			return (InterpolatePixel<T>)new BilinearPixel_U8();
		else if( type == ImageSInt16.class )
			return (InterpolatePixel<T>)new BilinearPixel_S16();
		else
			throw new RuntimeException("Unknown image type: "+type.getName());
	}
}
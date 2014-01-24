/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.segmentation;

import boofcv.core.image.ConvertBufferedImage;
import boofcv.factory.segmentation.FactorySegmentationAlg;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.*;
import org.ddogleg.struct.FastQueue;

import java.awt.image.BufferedImage;
import java.util.Random;

/**
 * @author Peter Abeles
 */
public class VisualizeSegmentMeanShiftApp {

	public static int spacialRadius = 6;
	public static float colorRadius = 15f;
	public static int minimumSize = 40;
	public static boolean fast = true;

	public static <T extends ImageBase> void process( BufferedImage image ,ImageType<T> type ) {
		T color = type.createImage(image.getWidth(),image.getHeight());

		ConvertBufferedImage.convertFrom(image, color, true);

		BufferedImage outColor = new BufferedImage(color.width,color.height,BufferedImage.TYPE_INT_RGB);
		BufferedImage outSegments = new BufferedImage(color.width,color.height,BufferedImage.TYPE_INT_RGB);

		SegmentMeanShift<T> alg =
				FactorySegmentationAlg.meanShift(spacialRadius, colorRadius, minimumSize , fast, type);

		long time0 = System.currentTimeMillis();
		alg.process(color);
		long time1 = System.currentTimeMillis();

		FastQueue<float[]> segmentColor = alg.getRegionColor();
		ImageSInt32 pixelToSegment = alg.getRegionImage();

		Random rand = new Random(234);

		int colors[] = new int[ segmentColor.size ];
		for( int i = 0; i < colors.length; i++ ) {
			colors[i] = rand.nextInt();
		}

		for( int y = 0; y < color.height; y++ ) {
			for( int x = 0; x < color.width; x++ ) {
				int index = pixelToSegment.unsafe_get(x,y);
				float []cv = segmentColor.get(index);

				int r,g,b;

				if( cv.length == 3 ) {
					r = (int)cv[0];
					g = (int)cv[1];
					b = (int)cv[2];
				} else {
					r = g = b = (int)cv[0];
				}

				int rgb = r << 16 | g << 8 | b;

				outColor.setRGB(x, y, colors[index]);
				outSegments.setRGB(x, y, rgb);
			}
		}

		System.out.println("Time MS "+(time1-time0));

		ShowImages.showWindow(outColor,"Regions");
		ShowImages.showWindow(outSegments,"Color of Segments");
	}

	public static void main(String[] args) {
//		BufferedImage image = UtilImageIO.loadImage("../data/evaluation/sunflowers.png");
//		BufferedImage image = UtilImageIO.loadImage("../data/evaluation/shapes01.png");
		BufferedImage image = UtilImageIO.loadImage("../data/applet/trees_rotate_01.jpg");
//		BufferedImage image = UtilImageIO.loadImage("../data/applet/segment/mountain_pines_people.jpg");
//		BufferedImage image = UtilImageIO.loadImage("/home/pja/Desktop/segmentation/example-orig.jpg");


		ImageType<MultiSpectral<ImageFloat32>> imageType = ImageType.ms(3,ImageFloat32.class);
//		ImageType<ImageFloat32> imageType = ImageType.single(ImageFloat32.class);

		process(image,imageType);
	}
}
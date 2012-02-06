/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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

package boofcv.alg.feature.detect.calibgrid;

import boofcv.alg.misc.ImageTestingOps;
import boofcv.struct.image.ImageFloat32;
import georegression.struct.point.Point2D_F64;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestRefineCornerSegmentFit {

	int width = 50;
	int height = 60;

	@Test
	public void stuff() {
		ImageFloat32 orig = new ImageFloat32(width,height);

		ImageTestingOps.fillRectangle(orig, 210, 0, 0, width, height);
		ImageTestingOps.fillRectangle(orig, 52, 20, 15, width, height);

		RefineCornerSegmentFit alg = new RefineCornerSegmentFit();

		alg.process(orig);

		Point2D_F64 corner = alg.getCorner();

		assertEquals(20,corner.getX(), 1e-8);
		assertEquals(15,corner.getY(),1e-8);

	}
}
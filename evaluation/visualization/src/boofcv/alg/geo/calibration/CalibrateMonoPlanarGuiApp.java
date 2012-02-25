/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.geo.calibration;

import boofcv.alg.distort.AddRadialDistortionPixel;
import boofcv.alg.distort.ImageDistort;
import boofcv.alg.distort.PointToPixelTransform_F32;
import boofcv.alg.interpolate.InterpolatePixel;
import boofcv.app.CalibrateMonoPlanarApp;
import boofcv.app.PlanarCalibrationDetector;
import boofcv.app.WrapPlanarGridTarget;
import boofcv.core.image.border.FactoryImageBorder;
import boofcv.core.image.border.ImageBorder;
import boofcv.factory.distort.FactoryDistort;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.distort.PixelTransform_F32;
import boofcv.struct.image.ImageFloat32;

import javax.swing.*;
import java.awt.*;

/**
 * Computes intrinsic camera calibration parameters from a set of calibration images.  Results
 * are displayed in a window allowing their accuracy to be easily seen.
 *
 * @author Peter Abeles
 */
public class CalibrateMonoPlanarGuiApp extends JPanel {

	// computes calibration parameters
	CalibrateMonoPlanarApp calibrator;
	// displays results
	MonoPlanarPanel gui = new MonoPlanarPanel();
	// needed by ProcessThread for displaying its dialog
	JPanel owner;

	// tells ProcessThread if it should be running or not
	boolean processing;

	// transform used to undistort image
	AddRadialDistortionPixel tran = new AddRadialDistortionPixel();
	ImageDistort<ImageFloat32> dist;

	
	public CalibrateMonoPlanarGuiApp(CalibrateMonoPlanarApp calibrator) {
		setLayout(new BorderLayout());
		setPreferredSize(new Dimension(800,525));
		this.calibrator = calibrator;
		this.owner = this;
		
		add(gui,BorderLayout.CENTER);

		// Distortion algorithm for removing radial distortion
		InterpolatePixel<ImageFloat32> interp = FactoryInterpolation.bilinearPixel(ImageFloat32.class);
		PixelTransform_F32 tran = new PointToPixelTransform_F32(this.tran);
		ImageBorder<ImageFloat32> border = FactoryImageBorder.value(ImageFloat32.class, 0);
		dist = FactoryDistort.distort(interp,border,ImageFloat32.class);
		dist.setModel(tran);
	}

	/**
	 * Processes all images in the directory.  Updates status of GUI while doing so
	 *
	 * @param directory Directory containing calibration images
	 */
	public void process( String directory ) {
		processing = true;
		calibrator.reset();
		new ProcessThread().start();
		
		calibrator.loadImages(directory);
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				gui.setImages(calibrator.getImageNames(),
						calibrator.getImages(),calibrator.getObservations());
			}});
		gui.repaint();

		calibrator.process();
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				gui.setResults(calibrator.getErrors());
				gui.setCalibration(calibrator.getFound());
			}});
		processing = false;

		// tell it how to undistort the image
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				ParametersZhang98 found = calibrator.getFound();

				tran.set(found.a,found.b,found.c,found.x0,found.y0,found.distortion);
				gui.setCorrection(dist);
		
				gui.repaint();
			}});
	}


	/**
	 * Displays a progress monitor and updates its state periodically
	 */
	public class ProcessThread extends Thread
	{
		ProgressMonitor progressMonitor;
		public ProcessThread() {
			progressMonitor = new ProgressMonitor(owner, "Computing Calibration", "", 0, 3);
		}

		@Override
		public void run() {
			while( processing ) {
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						progressMonitor.setProgress(calibrator.state);
						progressMonitor.setNote(calibrator.message);
					}});
				synchronized ( this ) {
					try {
						wait(100);
					} catch (InterruptedException e) {
					}
				}
			}
			progressMonitor.close();
		}
	}

	public static void main( String args[] ) {
//		PlanarCalibrationDetector detector = new WrapPlanarGridTarget(8,8);
		PlanarCalibrationDetector detector = new WrapPlanarGridTarget(3,4);
//		PlanarCalibrationDetector detector = new WrapPlanarChessTarget(3,4);

//		PlanarCalibrationTarget target = FactoryPlanarCalibrationTarget.gridSquare(8,8,0.5,7.0/18.0);
		PlanarCalibrationTarget target = FactoryPlanarCalibrationTarget.gridSquare(3,4,30,30);
//		PlanarCalibrationTarget target = FactoryPlanarCalibrationTarget.gridChess(3, 4, 30);

		CalibrateMonoPlanarApp calibrator = new CalibrateMonoPlanarApp(detector,true);
		calibrator.configure(target,true,2);
		
		CalibrateMonoPlanarGuiApp app = new CalibrateMonoPlanarGuiApp(calibrator);
		

		JFrame frame = new JFrame("Planar Calibration");
		frame.add(app, BorderLayout.CENTER);
		frame.pack();
		frame.setVisible(true);

//		app.process("../data/evaluation/calibration/mono/Sony_DSC-HX5V_Chess");
		app.process("../data/evaluation/calibration/mono/Sony_DSC-HX5V_Square");
//		app.process("../data/evaluation/calibration/mono/PULNiX_CCD_6mm_Zhang");

	}
}

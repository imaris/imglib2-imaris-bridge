import Imaris.IApplicationPrx;
import Imaris.IDataSetPrx;
import Imaris.cColorTable;
import Imaris.tType;
import ij.CompositeImage;
import ij.ImagePlus;
import ij.LookUpTable;
import ij.WindowManager;
import ij.gui.NewImage;
import ij.measure.Calibration;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import ij.process.LUT;
import java.awt.image.IndexColorModel;
import java.util.LinkedList;
import java.util.List;

class IceJUtils {

	public static List<ImagePlus> ImageFromDataSet(IDataSetPrx aDataSet, String aFileName, boolean aSplitChannels) throws Imaris.Error {
		if (aDataSet == null) {
			return null;
		}
		List<ImagePlus> vImages = new LinkedList<ImagePlus>();
		int vSizeC = aDataSet.GetSizeC();
		if (!aSplitChannels) {
			vImages.add(ImageFromDataSet(aDataSet, aFileName, 0, vSizeC));
		}
		else {
			for (int vIndexC = 0; vIndexC < vSizeC; vIndexC++) {
				vImages.add(ImageFromDataSet(aDataSet, aFileName, vIndexC, 1));
			}
		}
		return vImages;
	}

	static ImagePlus ImageFromDataSet(IDataSetPrx aDataSet, String aFileName, int aBeginC, int aSizeC) throws Imaris.Error {
		if (aDataSet == null) {
			return null;
		}
		int vSizeX = aDataSet.GetSizeX();
		int vSizeY = aDataSet.GetSizeY();
		int vSizeZ = aDataSet.GetSizeZ();
		int vSizeC = aSizeC;
		int vSizeT = aDataSet.GetSizeT();
		int vSizeZCT = vSizeZ * vSizeC * vSizeT;
		tType vType = aDataSet.GetType();
		ImagePlus vImage = null;

		String vName = aDataSet.GetParameter("Image", "Name");
		if (!vName.startsWith(aFileName)) {
			String vTmp = vName;
			vName = aFileName + " - " + vTmp;
		}
		if (aSizeC != aDataSet.GetSizeC()) {
			vName += " Ch " + (aBeginC + 1);
		}
		vName = UniqueIJName(vName);
		if (vType == tType.eTypeUInt8) {
			if (aSizeC == 0) { // do not use RGB images
				//if (aSizeC == 3) {
				vImage = NewImage.createRGBImage(vName, vSizeX, vSizeY, vSizeZCT / 3, NewImage.FILL_BLACK);
				vImage.setDimensions(1, vSizeZ, vSizeT);
				ImageProcessor vProcessor = vImage.getProcessor();
				ColorProcessor vColorProcessor = (ColorProcessor) vProcessor;
				for (int vIndexT = 0; vIndexT < vSizeT; vIndexT++) {
					for (int vIndexZ = 0; vIndexZ < vSizeZ; vIndexZ++) {
						byte[] vR = aDataSet.GetDataSubVolumeAs1DArrayBytes(0, 0, vIndexZ, 0, vIndexT, 0, 0, 1);
						byte[] vG = aDataSet.GetDataSubVolumeAs1DArrayBytes(0, 0, vIndexZ, 1, vIndexT, 0, 0, 1);
						byte[] vB = aDataSet.GetDataSubVolumeAs1DArrayBytes(0, 0, vIndexZ, 2, vIndexT, 0, 0, 1);
						vImage.setPosition(1, vIndexZ + 1, vIndexT + 1);
						vColorProcessor.setRGB(vR, vG, vB);
					}
				}
				// return, do not execute color to image
				ParametersToImage(aDataSet, vImage);
				return vImage;
			}
			vImage = NewImage.createByteImage(vName, vSizeX, vSizeY, vSizeZCT, NewImage.FILL_BLACK);
			vImage.setDimensions(vSizeC, vSizeZ, vSizeT);
			vImage = ColorsToImage(aDataSet, vImage, aBeginC);
			ImageProcessor vProcessor = vImage.getProcessor();
			for (int vIndexT = 0; vIndexT < vSizeT; vIndexT++) {
				for (int vIndexZ = 0; vIndexZ < vSizeZ; vIndexZ++) {
					for (int vIndexC = 0; vIndexC < vSizeC; vIndexC++) {
						byte[] vData = aDataSet.GetDataSubVolumeAs1DArrayBytes(0, 0, vIndexZ, aBeginC + vIndexC, vIndexT, 0, 0, 1);
						vImage.setPosition(vIndexC + 1, vIndexZ + 1, vIndexT + 1);
						vProcessor.setPixels(vData);
					}
				}
			}
		}
		else if (vType == tType.eTypeUInt16) {
			vImage = NewImage.createShortImage(vName, vSizeX, vSizeY, vSizeZCT, NewImage.FILL_BLACK);
			vImage.setDimensions(vSizeC, vSizeZ, vSizeT);
			vImage = ColorsToImage(aDataSet, vImage, aBeginC);
			ImageProcessor vProcessor = vImage.getProcessor();
			for (int vIndexT = 0; vIndexT < vSizeT; vIndexT++) {
				for (int vIndexZ = 0; vIndexZ < vSizeZ; vIndexZ++) {
					for (int vIndexC = 0; vIndexC < vSizeC; vIndexC++) {
						short[] vData = aDataSet.GetDataSubVolumeAs1DArrayShorts(0, 0, vIndexZ, aBeginC + vIndexC, vIndexT, 0, 0, 1);
						vImage.setPosition(vIndexC + 1, vIndexZ + 1, vIndexT + 1);
						vProcessor.setPixels(vData);
					}
				}
			}
		}
		else if (vType == tType.eTypeFloat) {
			vImage = NewImage.createFloatImage(vName, vSizeX, vSizeY, vSizeZCT, NewImage.FILL_BLACK);
			vImage.setDimensions(vSizeC, vSizeZ, vSizeT);
			vImage = ColorsToImage(aDataSet, vImage, aBeginC);
			ImageProcessor vProcessor = vImage.getProcessor();
			for (int vIndexT = 0; vIndexT < vSizeT; vIndexT++) {
				for (int vIndexZ = 0; vIndexZ < vSizeZ; vIndexZ++) {
					for (int vIndexC = 0; vIndexC < vSizeC; vIndexC++) {
						float[] vData = aDataSet.GetDataSubVolumeAs1DArrayFloats(0, 0, vIndexZ, aBeginC + vIndexC, vIndexT, 0, 0, 1);
						vImage.setPosition(vIndexC + 1, vIndexZ + 1, vIndexT + 1);
						vProcessor.setPixels(vData);
					}
				}
			}
		}
		ParametersToImage(aDataSet, vImage);
		return vImage;
	}

	private static String UniqueIJName(String aName) {
		int vId = 0;
		boolean vOk = true;
		int[] vIds = WindowManager.getIDList();
		if (vIds == null || vIds.length == 0) {
			return aName;
		}
		String vName = aName;
		while (vOk) {
			for (int vIndex : vIds) {
				ImagePlus vImage = WindowManager.getImage(vIndex);
				if (vImage.getTitle().equals(vName)) {
					vId++;
					vName = aName + "-" + vId;
					vOk = !vOk;
					break;
				}
			}
			vOk = !vOk;
		}
		return vName;
	}

	private static ImagePlus ColorsToImage(IDataSetPrx aDataSet, ImagePlus aImage, int aBeginC) throws Imaris.Error {
		CompositeImage vComposite = null;
		int vSizeC = aImage.getNChannels();
		if (vSizeC > 1) {
			aImage = vComposite = new CompositeImage(aImage);
		}
		ImageProcessor vProcessor = aImage.getProcessor();
		for (int vIndexC = 0; vIndexC < vSizeC; vIndexC++) {
			aImage.setPosition(vIndexC + 1, 1, 1);
			cColorTable vColorTable = aDataSet.GetChannelColorTable(aBeginC + vIndexC);
			IndexColorModel vColorModel = null;
			if (vColorTable != null && vColorTable.mColorRGB.length > 0) {
				vColorModel = ModelFromColorTable(vColorTable);
			}
			else {
				int vColor = aDataSet.GetChannelColorRGBA(aBeginC + vIndexC);
				vColorModel = ModelFromColor(vColor);
			}
			if (vComposite != null) {
				vComposite.setChannelColorModel(vColorModel);
			}
			else {
				vProcessor.setColorModel(vColorModel);
				if (vColorTable != null) {
					aImage.getProcessor().invertLut();
					aImage.getProcessor().invertLut();
				}
			}
			float vMin = aDataSet.GetChannelRangeMin(aBeginC + vIndexC);
			float vMax = aDataSet.GetChannelRangeMax(aBeginC + vIndexC);
			vProcessor.setMinAndMax(vMin, vMax);
			aImage.setDisplayRange(vMin, vMax);
		}
		return aImage;
	}

	public static IDataSetPrx DataSetFromImage(ImagePlus aImage, IApplicationPrx aApplication) throws Imaris.Error {
		if (aImage == null || aApplication == null) {
			return null;
		}
		int vSizeX = aImage.getWidth();
		int vSizeY = aImage.getHeight();
		int vSizeZ = aImage.getNSlices();
		int vSizeC = aImage.getNChannels();
		int vSizeT = aImage.getNFrames();
		ImageProcessor vProcessor = aImage.getProcessor();
		int vType = aImage.getType();
		IDataSetPrx vDataSet = aApplication.GetDataSet(); // try to preserve params
		if (vDataSet != null) {
			vDataSet = vDataSet.Clone();
		}
		else {
			vDataSet = aApplication.GetFactory().CreateDataSet();
		}
		vDataSet.SetParameter("Image", "Name", aImage.getTitle());
		if (vType == ImagePlus.COLOR_RGB) {
			RecreateDataSet(vDataSet, tType.eTypeUInt8, vSizeX, vSizeY, vSizeZ, vSizeC * 3, vSizeT);
			ColorProcessor vColorProcessor = (ColorProcessor) vProcessor;
			int vSizeXY = vSizeX * vSizeY;
			byte[] vR = new byte[vSizeXY];
			byte[] vG = new byte[vSizeXY];
			byte[] vB = new byte[vSizeXY];
			for (int vIndexT = 0; vIndexT < vSizeT; vIndexT++) {
				for (int vIndexZ = 0; vIndexZ < vSizeZ; vIndexZ++) {
					aImage.setPosition(1, vIndexZ + 1, vIndexT + 1);
					vColorProcessor.getRGB(vR, vG, vB);
					vDataSet.SetDataSubVolumeAs1DArrayBytes(vR, 0, 0, vIndexZ, 0, vIndexT, 0, 0, 1);
					vDataSet.SetDataSubVolumeAs1DArrayBytes(vG, 0, 0, vIndexZ, 1, vIndexT, 0, 0, 1);
					vDataSet.SetDataSubVolumeAs1DArrayBytes(vB, 0, 0, vIndexZ, 2, vIndexT, 0, 0, 1);
				}
			}
		}
		else if (vType == ImagePlus.GRAY8) {
			RecreateDataSet(vDataSet, tType.eTypeUInt8, vSizeX, vSizeY, vSizeZ, vSizeC, vSizeT);
			for (int vIndexT = 0; vIndexT < vSizeT; vIndexT++) {
				for (int vIndexZ = 0; vIndexZ < vSizeZ; vIndexZ++) {
					for (int vIndexC = 0; vIndexC < vSizeC; vIndexC++) {
						aImage.setPosition(vIndexC + 1, vIndexZ + 1, vIndexT + 1);
						//vIndexC + vIndexZ * vSizeC + vIndexT * vSizeC * vSizeZ + 1);
						vDataSet.SetDataSubVolumeAs1DArrayBytes((byte[]) vProcessor.getPixels(), 0, 0, vIndexZ, vIndexC, vIndexT, 0, 0, 1);
					}
				}
			}
		}
		else if (vType == ImagePlus.GRAY16) {
			RecreateDataSet(vDataSet, tType.eTypeUInt16, vSizeX, vSizeY, vSizeZ, vSizeC, vSizeT);
			for (int vIndexT = 0; vIndexT < vSizeT; vIndexT++) {
				for (int vIndexZ = 0; vIndexZ < vSizeZ; vIndexZ++) {
					for (int vIndexC = 0; vIndexC < vSizeC; vIndexC++) {
						aImage.setPosition(vIndexC + 1, vIndexZ + 1, vIndexT + 1);
						vDataSet.SetDataSubVolumeAs1DArrayShorts((short[]) vProcessor.getPixels(), 0, 0, vIndexZ, vIndexC, vIndexT, 0, 0, 1);
					}
				}
			}
		}
		else if (vType == ImagePlus.GRAY32) {
			RecreateDataSet(vDataSet, tType.eTypeFloat, vSizeX, vSizeY, vSizeZ, vSizeC, vSizeT);
			for (int vIndexT = 0; vIndexT < vSizeT; vIndexT++) {
				for (int vIndexZ = 0; vIndexZ < vSizeZ; vIndexZ++) {
					for (int vIndexC = 0; vIndexC < vSizeC; vIndexC++) {
						aImage.setPosition(vIndexC + 1, vIndexZ + 1, vIndexT + 1);
						vDataSet.SetDataSubVolumeAs1DArrayFloats((float[]) vProcessor.getPixels(), 0, 0, vIndexZ, vIndexC, vIndexT, 0, 0, 1);
					}
				}
			}
		}
		aImage.setPosition(1, 1, 1);
		ColorsToDataSet(aImage, vDataSet);
		ParametersToDataSet(aImage, vDataSet);
		return vDataSet;
	}

	private static void RecreateDataSet(IDataSetPrx aDataSet, tType aType, int aSizeX, int aSizeY, int aSizeZ, int aSizeC, int aSizeT) throws Imaris.Error {
		if (aDataSet.GetType() == aType) {
			aDataSet.Resize(0, aSizeX, 0, aSizeY, 0, aSizeZ, 0, aSizeC, 0, aSizeT);
		}
		else {
			aDataSet.Create(aType, aSizeX, aSizeY, aSizeZ, aSizeC, aSizeT);
		}
	}

	private static void ColorsToDataSet(ImagePlus aImage, IDataSetPrx aDataSet) throws Imaris.Error {
		int vType = aImage.getType();
		if (vType == ImagePlus.COLOR_RGB) {
			aDataSet.SetChannelColorRGBA(0, 255);
			aDataSet.SetChannelColorRGBA(1, 255 * 256);
			aDataSet.SetChannelColorRGBA(2, 255 * 256 * 256);
			aDataSet.SetChannelRange(0, (float) aImage.getDisplayRangeMin(), (float) aImage.getDisplayRangeMax());
			aDataSet.SetChannelRange(1, (float) aImage.getDisplayRangeMin(), (float) aImage.getDisplayRangeMax());
			aDataSet.SetChannelRange(2, (float) aImage.getDisplayRangeMin(), (float) aImage.getDisplayRangeMax());
		}
		else {
			int vSizeC = aImage.getNChannels();
			ImageProcessor vProcessor = aImage.getProcessor();
			for (int vIndexC = 0; vIndexC < vSizeC; vIndexC++) {
				aImage.setPosition(vIndexC + 1, 1, 1);
				cColorTable vColor = GetLUT(aImage);
				if (vColor.mColorRGB.length == 1) {
					aDataSet.SetChannelColorRGBA(vIndexC, vColor.mColorRGB[0]);
				}
				else {
					aDataSet.SetChannelColorTable(vIndexC, vColor.mColorRGB, vColor.mAlpha);
				}
				aDataSet.SetChannelRange(vIndexC, (float) vProcessor.getMin(), (float) vProcessor.getMax());
				aDataSet.SetChannelRange(vIndexC, (float) aImage.getDisplayRangeMin(), (float) aImage.getDisplayRangeMax());
			}
		}
	}

	static float[] GetRGBA(int aRGBA)
	{
		int vInt = aRGBA;
		float[] vRGBA = new float[4];
		for (int i = 0; i < 4; ++i) {
			vRGBA[i] = Value((byte) (vInt % 256)) / 255.0f;
			vInt /= 256;
		}
		return vRGBA;
	}

	static IndexColorModel ModelFromColor(int aRGBA) {
		int vSize = 256;

		byte[] rLut = new byte[vSize];
		byte[] gLut = new byte[vSize];
		byte[] bLut = new byte[vSize];
		byte[] aLut = new byte[vSize];

		float[] vRGBA = GetRGBA(aRGBA);
		for (int i = 0; i < vSize; ++i) {
			rLut[i] = (byte) (i * vRGBA[0]);
			gLut[i] = (byte) (i * vRGBA[1]);
			bLut[i] = (byte) (i * vRGBA[2]);
			aLut[i] = (byte) (i * vRGBA[3]);
		}
		return new IndexColorModel(8, vSize, rLut, gLut, bLut, aLut);
	}

	static IndexColorModel ModelFromColorTable(cColorTable aColor) {
		int[] vRGB = aColor.mColorRGB;
		int vSize = 256;
		int vSourceSize = vRGB.length;

		byte[] rLut = new byte[vSize];
		byte[] gLut = new byte[vSize];
		byte[] bLut = new byte[vSize];
		byte[] aLut = new byte[vSize];

		for (int i = 0; i < vSize; ++i) {
			int vIndex = (i * vSourceSize) / vSize;
			float[] vRGBA = GetRGBA(vRGB[vIndex]);
			rLut[i] = (byte) (vRGBA[0] * 255);
			gLut[i] = (byte) (vRGBA[1] * 255);
			bLut[i] = (byte) (vRGBA[2] * 255);
			aLut[i] = aColor.mAlpha;
		}
		return new IndexColorModel(8, vSize, rLut, gLut, bLut, aLut);
	}

	static cColorTable GetLUT(ImagePlus aImage) {
		cColorTable vColorRGB = new cColorTable();
		byte[] vR = null;
		byte[] vG = null;
		byte[] vB = null;
		if (aImage.isComposite()) {
			CompositeImage vComposite = (CompositeImage) aImage;
			LUT vTable = vComposite.getChannelLut();
			int vSize = vTable.getMapSize();
			vR = new byte[vSize];
			vG = new byte[vSize];
			vB = new byte[vSize];
			vTable.getReds(vR);
			vTable.getGreens(vG);
			vTable.getBlues(vB);
		}
		else {
			LookUpTable vTable = aImage.createLut();
			if (vTable == null) {
				return null;
			}
			vR = vTable.getReds();
			vG = vTable.getGreens();
			vB = vTable.getBlues();
		}

		int vIndexBegin = 0;
		int vSize = vR.length;

		if (IsPureColor(vR) && IsPureColor(vG) && IsPureColor(vB)) {
			// use only the last entry of the table
			vIndexBegin = vSize - 1;
			vSize = 1;
		}

		vColorRGB.mColorRGB = new int[vSize];
		int[] vRGB = vColorRGB.mColorRGB;
		for (int vIndex = 0; vIndex < vSize; vIndex++) {
			int vColor = Value(vR[vIndexBegin + vIndex]);
			vColor += 256 * Value(vG[vIndexBegin + vIndex]);
			vColor += 256 * 256 * Value(vB[vIndexBegin + vIndex]);
			vColorRGB.mColorRGB[vIndex] = vColor;
		}
		return vColorRGB;
	}

	static boolean IsPureColor(byte[] aTable) {
		int vSize = aTable.length;
		int vColor = Value(aTable[vSize - 1]);
		for (int vIndex = 0; vIndex < vSize; vIndex++) {
			if (Value(aTable[vIndex]) != vColor * vIndex / 255) {
				return false;
			}
		}
		return true;
	}

	static int Value(byte aValue) {
		int vValue = aValue;
		if (vValue < 0) {
			vValue += 256;
		}
		return vValue;
	}

	static void ParametersToImage(IDataSetPrx aDataSet, ImagePlus aImage) throws Imaris.Error {
		Calibration vCal = aImage.getCalibration();
		vCal.xOrigin = aDataSet.GetExtendMinX();
		vCal.yOrigin = aDataSet.GetExtendMinY();
		vCal.zOrigin = aDataSet.GetExtendMinZ();
		vCal.pixelWidth = (aDataSet.GetExtendMaxX() - aDataSet.GetExtendMinX()) / aDataSet.GetSizeX();
		vCal.pixelHeight = (aDataSet.GetExtendMaxY() - aDataSet.GetExtendMinY()) / aDataSet.GetSizeY();
		vCal.pixelDepth = (aDataSet.GetExtendMaxZ() - aDataSet.GetExtendMinZ()) / aDataSet.GetSizeZ();
		vCal.frameInterval = aDataSet.GetTimePointsDelta();
		vCal.setUnit(aDataSet.GetUnit());
	}

	static void ParametersToDataSet(ImagePlus aImage, IDataSetPrx aDataSet) throws Imaris.Error {
		// assume aImage.sCalibrated
		Calibration vCal = aImage.getCalibration();
		aDataSet.SetExtendMinX((float) vCal.xOrigin);
		aDataSet.SetExtendMinY((float) vCal.yOrigin);
		aDataSet.SetExtendMinZ((float) vCal.zOrigin);
		aDataSet.SetExtendMaxX((float) (vCal.xOrigin + vCal.pixelWidth * aImage.getWidth()));
		aDataSet.SetExtendMaxY((float) (vCal.yOrigin + vCal.pixelHeight * aImage.getHeight()));
		aDataSet.SetExtendMaxZ((float) (vCal.zOrigin + vCal.pixelDepth * aImage.getNSlices()));
		String vUnit = vCal.getUnit().replace('\u00B5', 'u');
		aDataSet.SetTimePointsDelta(vCal.frameInterval);
		aDataSet.SetUnit(vUnit);
	}
}

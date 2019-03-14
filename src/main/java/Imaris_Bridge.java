import Imaris.IApplicationPrx;
import Imaris.IApplicationPrxHelper;
import Imaris.IDataSetPrx;
import com.bitplane.xt.IceClient;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import java.io.File;
import java.util.LinkedList;
import java.util.List;

public class Imaris_Bridge implements PlugIn {
	//	public class Imaris_Bridge extends PlugInFrame {
	private static final long serialVersionUID = 1L;

	static final String[] mCommandsString = { "None", "Plugin", "In", "Out", "SendNewFile", "Quit", "Log", "WaitCloseWindow", "CloseWindow", "CloseAllWindows", "ShowIJ", "CloseIn", "SelectAll", "EndPoints", "ApplicationID", "AskAppID", "SetTool", "Terminate" };

	static class cCommand {
		public cCommand(String aName) {
			mName = aName;
		}

		public String mName = "";
		public String mParams = "";
	}

	List<cCommand> mCommands = null;

	StringBuilder mLog = new StringBuilder();
	IceClient mIceClient = null;
	List<ImagePlus> mImageIn = null;
	String mEndPoints = "default -p 4029";
	int mObjectId = 0;

	public Imaris_Bridge() {
		//		super("Imaris_Bridge");

		Log("Imaris Bridge - Log Window\n");

		//		if (Macro.getOptions() != null) {
		//			mCommands = CommandsFromString(Macro.getOptions());
		//		}
		//		else {
		////			ShowLog();
		////			Log("Configuration options not provided.");
		////			Log("Imaris_Bridge should be started indirectly by Imaris");
		//			IJ.showMessage("Missing parameters. Could not connect to Imaris.");
		//		}

		//IJ.getInstance().setVisible(false);
	}

	public static void Do(String args)
	{
		Imaris_Bridge b = new Imaris_Bridge();
		b.run(args);
	}

	public static void In(String args)
	{
		Do(args + "In");
	}

	public static void Out(String args)
	{
		Do(args + "Out");
	}

	public static void SendNewFile(String args)
	{
		Do(args + "SendNewFile");
	}

	public static void Terminate(String args)
	{
		Do(args + "Terminate");
	}

	public void run(String arg) {
		if (arg != null && arg.length() > 0) {
			mCommands = CommandsFromString(arg);
		}
		try {
			Run();
		}
		catch (Exception e) {
			CatchException(e.getMessage());
		}
	}

	private void CatchException(String aMessage) {
		if (mCommands != null && mCommands.size() > 0 && mCommands.get(mCommands.size() - 1).mName == "Quit") {
			Quit(aMessage);
		}
		else {
			Log("Imaris_Bridge: An error occurred");
			Log(aMessage);
			ShowLog();
		}
	}

	void Run() {
		if (mCommands == null) {
			return;
		}
		for (cCommand vCommand : mCommands) {
			Log(vCommand.mName);
			Run(vCommand);
		}
	}

	void Run(cCommand aCommand) {
		if (aCommand.mName == "None") {
		}
		else if (aCommand.mName == "Plugin") {
			DoPlugin(aCommand.mParams);
		}
		else if (aCommand.mName == "In") {
			DoIn(aCommand.mParams);
		}
		else if (aCommand.mName == "Out") {
			DoOut(aCommand.mParams, false);
		}
		else if (aCommand.mName == "SendNewFile") {
			DoOut(aCommand.mParams, true);
		}
		else if (aCommand.mName == "Quit") {
			Quit();
		}
		else if (aCommand.mName == "Log") {
			ShowLog();
		}
		else if (aCommand.mName == "WaitCloseWindow") {
			DoLoop(aCommand.mParams);
		}
		else if (aCommand.mName == "CloseWindow") {
			DoCloseWindow();
		}
		else if (aCommand.mName == "CloseAllWindows") {

		}
		else if (aCommand.mName == "ShowIJ") {
			IJ.getInstance().setVisible(true);
		}
		else if (aCommand.mName == "CloseIn") {
			DoCloseIn();
		}
		else if (aCommand.mName == "SelectAll") {
			DoMakeRect();
		}
		else if (aCommand.mName == "EndPoints") {
			DoSetEndPoints(aCommand.mParams);
		}
		else if (aCommand.mName == "ApplicationID") {
			DoSetID(aCommand.mParams);
		}
		else if (aCommand.mName == "AskAppID") {
			DoAskID();
		}
		else if (aCommand.mName == "SetTool") {
			DoSetTool(aCommand.mParams);
		}
		else if (aCommand.mName == "Terminate") {
			DoTerminate(aCommand.mParams);
		}
	}

	private IApplicationPrx GetApplication() {
		ImarisServer.IServerPrx vServer = GetIceClient().GetServer();
		return vServer == null ? null : GetApp(vServer, mObjectId);
	}

	private static IApplicationPrx GetApp(ImarisServer.IServerPrx aServer, int aObjectId) {
		Ice.ObjectPrx vObject = aServer.GetObject(aObjectId);
		return IApplicationPrxHelper.checkedCast(vObject);
	}

	private void DoTerminate(String mParams) {
		IApplicationPrx vApplication = GetApplication();
		if (vApplication == null) {
			Log(GetIceClient().GetError());
			return;
		}
		//vApplication.TerminateConnection();
		CloseIceClient();
	}

	private void DoCloseWindow() {
		IJ.run("Close");
	}

	//	private void DoCloseAllWindows() {
	//		while (WindowManager.getWindowCount() > 0) {
	//			DoCloseWindow();
	//		}
	//	}

	private void DoSetTool(String aParams) {
		IJ.setTool(aParams);
	}

	private void DoSetEndPoints(String aParams) {
		if (aParams != null) {
			mEndPoints = aParams;
		}
	}

	private void DoSetID(String aParams) {
		if (aParams != null) {
			try {
				mObjectId = Integer.parseInt(aParams);
			}
			catch (NumberFormatException vError) {
				Log(vError.toString());
			}
		}
	}

	private void DoAskID() {
		ImarisServer.IServerPrx vServer = GetIceClient().GetServer();
		if (vServer == null) {
			IJ.showMessage("Could not connect to Imaris");
			return;
		}
		int vCount = vServer.GetNumberOfObjects();
		if (vCount == 0) {
			IJ.showMessage("Could not connect to Imaris");
			return;
		}
		if (vCount == 1) {
			mObjectId = vServer.GetObjectID(0);
			return;
		}
		int[] vIds = new int[vCount];
		String[] vInstances = new String[vCount];
		for (int vIndex = 0; vIndex < vCount; vIndex++) {
			vIds[vIndex] = vServer.GetObjectID(vIndex);
			String vDescription = "";
			try {
				IApplicationPrx vApp = GetApp(vServer, vIds[vIndex]);
				vDescription = vApp.GetVersion() + " " + vApp.GetCurrentFileName();
			}
			catch (Imaris.Error vError) {
				vDescription += " [ CONNECTION ERROR ]";
			}
			vInstances[vIndex] = "Id " + vIds[vIndex] + ": " + vDescription;
		}
		GenericDialog vDialog = new GenericDialog("Connect to Imaris");
		vDialog.addChoice("", vInstances, vInstances[0]);
		vDialog.showDialog();
		if (!vDialog.wasCanceled()) {
			mObjectId = vIds[vDialog.getNextChoiceIndex()];
		}
	}

	private void DoMakeRect() {
		ImagePlus vImage = WindowManager.getCurrentImage();
		if (vImage != null) {
			IJ.makeRectangle(0, 0, vImage.getWidth(), vImage.getHeight());
		}
	}

	private void DoLoop(String aOptions) {
		int vWindows = GetWindowsCount();
		Log("Windows open:" + vWindows);
		int vTarget = vWindows - 1;
		while (vWindows > vTarget) {
			DoEvents();
			int vCurrent = GetWindowsCount();
			if (vCurrent != vWindows) {
				vWindows = vCurrent;
				Log("Windows open:" + vWindows);
			}
		}
	}

	private void DoOut(String aOptions, Boolean aMarkAsNewImage) {
		try {
			// TODO: merge channels or ask which image if size do not match!
			//		int vSize = WindowManager.getImageCount();
			//		if (vSize > 1) {
			//			int[] vIds = WindowManager.getIDList();
			//			for (int vIndex : vIds) {
			//				ImagePlus vImage = WindowManager.getImage(vIndex);
			ImagePlus vImageOut = WindowManager.getCurrentImage();
			if (vImageOut == null) {
				Log("No image to export");
				return;
			}
			IApplicationPrx vApplication = GetApplication();
			if (vApplication == null) {
				Log(GetIceClient().GetError());
				Quit("Could not connect to Imaris");
				return;
			}
			Log("Write image");
			Log("Stack " + vImageOut.getStack().getSize());
			int vSizeZ = vImageOut.getNSlices();
			int vSizeC = vImageOut.getNChannels();
			int vSizeT = vImageOut.getNFrames();
			Log("SizeZ " + vSizeZ);
			Log("SizeC " + vSizeC);
			Log("SizeT " + vSizeT);
			Log("Type " + vImageOut.getType() + " (" + ImagePlus.GRAY8 + ", " + ImagePlus.GRAY16 + ", " + ImagePlus.GRAY32 + ", " + ImagePlus.COLOR_256 + ", " + ImagePlus.COLOR_RGB + ")");
			//		IJ.showMessage("" + WindowManager.getImageCount());
			IDataSetPrx vDataSet = IceJUtils.DataSetFromImage(vImageOut, vApplication);
			if (aMarkAsNewImage) {
				vDataSet.SetParameter("Fiji", "MarkAsNewImage", "true");
			}
			//		IJ.wait(10000);
			vApplication.DataSetPushUndo("ImageJPlugin");
			vApplication.SetDataSet(vDataSet);
			CloseIceClient();
		}
		catch (Imaris.Error vError) {
			DisplayError(vError);
		}
	}

	private void DoIn(String aOptions) {
		try {
			IApplicationPrx vApplication = GetApplication();
			if (vApplication == null) {
				Log(GetIceClient().GetError());
				Quit("Could not connect to Imaris");
				return;
			}
			boolean vSplit = aOptions != null && aOptions.toLowerCase().equals("splitchannels");
			File vCurrentPath = new File(vApplication.GetCurrentFileName());
			String vFilename = vCurrentPath.getName();
			mImageIn = IceJUtils.ImageFromDataSet(vApplication.GetDataSet(), vFilename, vSplit);
			if (mImageIn == null) {
				Quit("Could not get image from Imaris");
				return;
			}
			Log("Show image");
			for (ImagePlus vImage : mImageIn) {
				vImage.setPosition(1, 1, 1);
				vImage.show();
				vImage.updateAndDraw();
			}
			CloseIceClient();
		}
		catch (Imaris.Error vError) {
			DisplayError(vError);
		}
	}

	private void DisplayError(Imaris.Error aError) {
		IJ.showMessage("Imaris_Bridge: An error occurred\n\n" + "Type: " + aError.mType + "Description: " + aError.mDescription + "Location: " + aError.mLocation);
	}

	private void DoCloseIn() {

		for (ImagePlus vImage : mImageIn) {
			if (vImage != null) {
				WindowManager.setCurrentWindow(vImage.getWindow());
				IJ.run("Close");
			}
		}
		mImageIn = null;
	}

	private void DoPlugin(String aPlugin) {
		if (aPlugin.length() > 0) {
			IJ.run(aPlugin);
		}
	}

	private void ShowLog() {
		String vLog = mLog.toString();
		if (vLog.length() > 0) {
			IJ.showMessage(vLog);
		}
	}

	void Quit() {
		Quit(null);
	}

	void Quit(String aReason) {
		if (aReason != null && aReason.length() > 0) {
			IJ.showMessage(aReason);
		}
		//		Log("Quit");
		//		DoCloseAllWindows();
		//		IJ.run("Quit");
	}

	IceClient GetIceClient() {
		if (mIceClient == null) {
			mIceClient = new IceClient("ImarisServer", mEndPoints, 10000);
		}
		return mIceClient;
	}

	void CloseIceClient() {
		if (mIceClient != null) {
			mIceClient.Terminate();
			mIceClient = null;
		}
	}

	void DoEvents() {
		IJ.wait(100);
	}

	void Log(String aText) {
		//mLog.append(aText + "\n");
	}

	int GetWindowsCount() {
		//return CountFrames() - MyWindowsCount();
		return CountWindows() - MyWindowsCount();
		//return CountIJWindows() - MyWindowsCount();
	}

	int MyWindowsCount() {
		return 0; // not working :( isShowing() ? 1 : 0;
	}

	static int CountFrames() {
		int vCount = 0;
		//		for (Frame vFrame : getFrames()) {
		//			if (vFrame.isVisible()) {
		//				vCount++;
		//			}
		//		}
		return vCount;
	}

	static int CountWindows() {
		int vCount = 0;
		//		for (Window vWindow : getWindows()) {
		//			if (vWindow.isVisible()) {
		//				vCount++;
		//			}
		//		}
		return vCount;
	}

	static int CountIJWindows() {
		return WindowManager.getWindowCount();
	}

	List<cCommand> CommandsFromString(String aOptions) {
		List<cCommand> vResult = new LinkedList<cCommand>();
		int vIndex = -1;
		for (String vCommand : mCommandsString) {
			if (aOptions.toUpperCase().startsWith(vCommand.toUpperCase())) {
				vIndex = 0;
				aOptions = "-" + aOptions;
				break;
			}
		}
		while (vIndex >= 0) {
			vIndex++;
			int vEnd0 = aOptions.indexOf("-", vIndex); // other command
			int vEnd1 = aOptions.indexOf("\"", vIndex); // params
			int vEnd = vEnd0;
			if (vEnd0 < 0 && vEnd1 < 0) {
				vEnd = aOptions.length();
			}
			else if (vEnd0 < 0 || (vEnd1 >= 0 && vEnd1 < vEnd0)) {
				vEnd = vEnd1;
			}
			String vTag = aOptions.substring(vIndex, vEnd);
			cCommand vNew = null;
			for (String vCommand : mCommandsString) {
				if (vTag.toUpperCase().startsWith(vCommand.toUpperCase())) {
					vNew = new cCommand(vCommand);
					vResult.add(vNew);
					Log("-" + vCommand);
					if (vTag.length() > vCommand.length()) {
						String vParams = vTag.substring(vCommand.length());
						vNew.mParams = vParams;
						Log("\"" + vParams + "\"");
					}
					break;
				}
			}
			vIndex = vEnd;
			if (vEnd == vEnd1) {
				vEnd = aOptions.indexOf("\"", vIndex + 1);
				if (vEnd >= 0) {
					if (vNew != null) {
						String vParams = aOptions.substring(vIndex + 1, vEnd);
						vNew.mParams = vParams;
						Log("\"" + vParams + "\"");
					}
					vIndex = vEnd + 1;
				}
			}
			vIndex = aOptions.indexOf("-", vIndex);
		}
		return vResult;
	}

}

package com.bitplane.xt;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import Ice.InitializationData;
import Ice.Properties;
import ImarisServer.*;

public class IceClient {
	Ice.Communicator mCommunicator = null;
	IServerPrx mServer = null;
	String mError = "";

	public IceClient(String aName, String aEndPoints, int aServerTimeoutMillisec) {
		mError = "";
		try {
			InitializationData vData = new InitializationData();
			Properties vProperties = Ice.Util.createProperties();
			vProperties.setProperty("Ice.Default.EncodingVersion", "1.0");
			vProperties.setProperty("Ice.MessageSizeMax", "1000000000");
			String vRetryIntervals = "0";
			for (int vIndex = 0; vIndex < 5; vIndex++) {
				vRetryIntervals += " " + (aServerTimeoutMillisec * (vIndex * 2 + 1) / 25);
			}
			vProperties.setProperty("Ice.RetryIntervals", vRetryIntervals);
			vData.properties = vProperties;
			mCommunicator = Ice.Util.initialize(vData);
			Ice.ObjectPrx vObject = mCommunicator.stringToProxy(aName + ":" + aEndPoints);
			mServer = IServerPrxHelper.checkedCast(vObject);
		}
		catch (Ice.LocalException e) {
			e.printStackTrace(new PrintStream(new OutputStream() {
				public void write(int arg0) throws IOException {
					mError += (char) arg0;
				}
			}));
		}
		catch (Exception e) {
			mError += e.getMessage();
		}
	}

	public void Terminate() {
		mError = "";
		if (mCommunicator != null) {
			try {
				mCommunicator.shutdown();
				mCommunicator.destroy();
			}
			catch (Exception e) {
				mError = e.getMessage();
			}
		}
	}

	public IServerPrx GetServer() {
		return mServer;
	}

	public String GetError() {
		return mError;
	}

}

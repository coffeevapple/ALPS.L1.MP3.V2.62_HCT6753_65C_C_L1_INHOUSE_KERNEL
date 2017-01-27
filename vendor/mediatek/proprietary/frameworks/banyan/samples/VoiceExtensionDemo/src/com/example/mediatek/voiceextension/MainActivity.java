/*
 * MediaTek (Author: Clark Shih)
 * Android sample codes for voice interface extension, VIE SDK
 */
package com.example.mediatek.voiceextension;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import android.app.Activity;
import android.content.res.Resources.NotFoundException;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

//import VIE SDK components
import com.mediatek.compatibility.voiceextension.VoiceExtensionSupport;
import com.mediatek.voiceextension.VoiceCommandManager;
import com.mediatek.voiceextension.VoiceCommandListener;
import com.mediatek.voiceextension.VoiceCommandResult;

public class MainActivity extends Activity 
{
	private boolean recognitionStarted_ = false;         //a boolean flag which indicates the recognition is started or not 
	private boolean settingCommandIsProcessing_ = false; //if "setting command" is still processing, this value is true, for flow control 

	private static final String commandSetName_ = "camera"; /** To define commands, a set is needed; this String defines set's name 
	                                                         * The name cannot contain other characters except English letters and Arabic numerals. 
	                                                         * The length is limited to 32. 
	                                                         */

	/**
	 * define the protocol between ResponseListener (an extended VoiceCommandListener) and Activity's Handler 
	 * the defined values are used for message's what
	 */
	private static final int API_COMMAND_START_RECOGNITION = 1;
	private static final int API_COMMAND_STOP_RECOGNITION = 2;
	private static final int API_COMMAND_RECOGNIZE_RESULT = 3;
	private static final int API_COMMAND_NOTIFY_ERROR = 4;
	private static final int API_COMMAND_SET_COMMANDS = 5;

	/**
	 * wrap function for putting information on the EditText, which
	 * shows all information on the screen   
	 */
	private void putInfo_(String info)
	{
		EditText edtInfo = (EditText)findViewById(R.id.edtInfo);
		edtInfo.append("\n.");
		edtInfo.append(info);
	}

	/**
	 * define the response to "Start Recognition":
	 * switch recognitionStarted_ value, and control UI
	 */
	private void respondToStart_(int retCode)
	{
		if(VoiceCommandResult.SUCCESS == retCode) 
		{
			recognitionStarted_ = true;
			putInfo_("voice recognition starts");
			Button btnSwitchRecognition = (Button)findViewById(R.id.btnSwitchStartStop);
			btnSwitchRecognition.setText("Stop");			
		}
	}

	/**
	 * define the response to "Stop Recognition":
	 * switch recognitionStarted_ value, and control UI
	 */
	private void respondToStop_(int retCode)
	{
		if(VoiceCommandResult.SUCCESS == retCode) 
		{
			recognitionStarted_ = false;
			putInfo_("voice recognition stops");
			Button btnSwitchRecognition = (Button)findViewById(R.id.btnSwitchStartStop);
			btnSwitchRecognition.setText("Start");			
		}
	}

	/**
	 * define the response to "Set Commands":
	 * control UI
	 */
	private void respondToSetCommands_(int retCode)
	{
		if(VoiceCommandResult.SUCCESS == retCode) 
		{
			settingCommandIsProcessing_ = false;
			putInfo_("setting command done, commands are: " + stringArrayToString_(getCommands_(), ","));
		}
	}

	/**
	 * define the Handler, which receives message from ResponseListener and performs corresponding behaviors    
	 */
	public Handler handler_ = new Handler() 
	{
		@Override
		public void handleMessage(Message msg) 
		{
			Bundle b = msg.getData();
			int retCode = b.getInt("retCode");

			switch (msg.what) 
			{
			case API_COMMAND_START_RECOGNITION:
				respondToStart_(retCode);
				break;

			case API_COMMAND_STOP_RECOGNITION:
				respondToStop_(retCode);
				break;

			case API_COMMAND_RECOGNIZE_RESULT:
				String commandStr = b.getString("commandStr");
				putInfo_("recognized command: " + commandStr);
				break;

			case API_COMMAND_NOTIFY_ERROR:
				putInfo_("error with code: " + retCode);
				break;

			case API_COMMAND_SET_COMMANDS:
				respondToSetCommands_(retCode);
				break;

			default:
				break;
			}
		};
	};	

	/**
	 * wrap function for checking compatibility of VIE SDK APIs
	 */
	private boolean checkCompatibility_()
	{
		if(VoiceExtensionSupport.isVoiceExtensionFeatureAvailable())
		{
			putInfo_("VIE APIs are supported on this phone!");
			return true;
		}
		else
		{
			putInfo_("VIE APIs are not supported on this phone!");
			return false;	
		}
	}

	/**
	 * wrap function for getting VoiceCommandManager instance, with showing info if the instance does not exist
	 */
	private VoiceCommandManager getVoiceCommandManagerInstance_()
	{
		VoiceCommandManager vcm = VoiceCommandManager.getInstance();
		if(vcm==null)
		{putInfo_("VoiceCommandManager instance is not found");}

		return vcm;
	}


	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		if(!checkCompatibility_())
		{return;}

		VoiceCommandManager vcm = getVoiceCommandManagerInstance_();
		if(vcm==null)
		{return;}

		boolean commandSetExist = (VoiceCommandResult.COMMANDSET_ALREADY_EXIST == vcm.isCommandSetCreated(commandSetName_));

		if(!commandSetExist)
		{vcm.createCommandSet(commandSetName_);}

		ResponseListener listener = new ResponseListener(); //create a response listener for receiving callback from VoiceCommandManager   
		vcm.selectCurrentCommandSet(commandSetName_, listener); //select current command set, and bind the set with the listener

		if(!commandSetExist) //if the set does not exist before, then commands should put into the command set
		{
			settingCommandIsProcessing_ = true;
			setCommandWrap_();
		}
		else
		{
			putInfo_("command set exists, commands are: " + stringArrayToString_(getCommands_(), ","));
		}

		//setup the button
		Button btnSwitchRecognition = (Button)findViewById(R.id.btnSwitchStartStop);
		btnSwitchRecognition.setOnClickListener(new Button.OnClickListener()
		{
			@Override
			public void onClick(View v) 
			{
				VoiceCommandManager vcm = getVoiceCommandManagerInstance_();
				if(vcm==null)
				{return;}

				if(recognitionStarted_) 
				{
					//if the recognition is started, then stop the recognition
					try 
					{vcm.stopRecognition();} 
					catch(IllegalAccessException e) 
					{e.printStackTrace();}
				}
				else
				{
					//if setting command is still processing, the recognition cannot be started
					if(settingCommandIsProcessing_)
					{
						putInfo_("setting command is processing, please wait");
						return;
					}

					//if the recognition is not started, then start the recognition
					try 
					{vcm.startRecognition();} 
					catch(IllegalAccessException e) 
					{e.printStackTrace();} 
				}
			}
		});
	}

	@Override
	protected void onPause()
	{
		super.onPause();
		
		//if the recognition is started, the recognition must be stopped when the Activity is paused
		if(recognitionStarted_)
		{
			VoiceCommandManager vcm = getVoiceCommandManagerInstance_();
			try 
			{vcm.stopRecognition();} 
			catch(IllegalAccessException e) 
			{e.printStackTrace();}
		}
	}

	/**
	 * wrap function for getting commands in String array
	 */
	private String[] getCommands_()
	{
		VoiceCommandManager vcm = getVoiceCommandManagerInstance_();
		if(vcm==null)
		{return null;}

		try 
		{return vcm.getCommands();} 
		catch (IllegalAccessException e) 
		{
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * wrap function for transforming String array into one String with delimiter ", " 
	 */
	private String stringArrayToString_(String[] strings, String delimiter)
	{
		if((null==strings) || (0==strings.length))
		{return "";}
		else
		{
			String result = strings[0];
			for(int i=1;i<strings.length;++i)
			{result+=(", " + strings[i]);}
			return result;
		}
	}

	/**
	 * wrap function for sending message with retCode from the ResponseListener
	 */
	private void sendRetCode_(int api, int retCode)
	{
		Message m = new Message();
		Bundle b = new Bundle();
		b.putInt("retCode", retCode);

		m.what = api;
		m.setData(b);
		handler_.sendMessage(m);
	}

	/**
	 * the response listener, which defines behaviors when VoiceCommandManager performs callback
	 */
	class ResponseListener extends VoiceCommandListener
	{
		@Override
		public void onCommandRecognized(int retCode, int commandId, String commandStr)
		{
			Message m = new Message();
			Bundle b = new Bundle();
			b.putInt("retCode", retCode);
			b.putInt("commandId", commandId);
			b.putString("commandStr", commandStr);

			m.what = API_COMMAND_RECOGNIZE_RESULT;
			m.setData(b);
			handler_.sendMessage(m);
		}

		@Override
		public void onSetCommands(int retCode)
		{sendRetCode_(API_COMMAND_SET_COMMANDS, retCode);}

		@Override
		public void onStartRecognition(int retCode)
		{sendRetCode_(API_COMMAND_START_RECOGNITION, retCode);}

		@Override
		public void onStopRecognition(int retCode)
		{sendRetCode_(API_COMMAND_STOP_RECOGNITION, retCode);}

		@Override
		public void onError(int retCode)
		{sendRetCode_(API_COMMAND_NOTIFY_ERROR, retCode);}

		@Override
		public void onPauseRecognition(int retCode) //do nothing since VoiceCommandManager's pause function is not used 
		{}

		@Override
		public void onResumeRecognition(int retCode) //do nothing since VoiceCommandManager's resume function is not used 
		{}
	}


	// following are command setting mechanism 

	/**
	 * define the indices of command setting method  
	 */
	private enum SetCommandMethod 
	{BY_STRING, BY_FILE_PATH, BY_FILE_ASSETS, BY_FILE_RAW};

	/**
	 * define the indices of command setting method  
	 */
	private void setCommandWrap_()
	{
		VoiceCommandManager vcm = getVoiceCommandManagerInstance_();
		if(vcm==null)
		{return;}
		
		SetCommandMethod method = SetCommandMethod.BY_STRING; //please assign the desired method; here "by string" is default 
		
		if(SetCommandMethod.BY_STRING == method)
		{
			//the command stings can be defined by users
			String[] commandArray = {"capture","cheese"};  

			try 
			{vcm.setCommands(commandArray);} 
			catch (IllegalAccessException e) 
			{e.printStackTrace();}
		}
		else if(SetCommandMethod.BY_FILE_PATH == method)
		{
			//do not use this method if the path is not accessible or the .xmf file is not available
			String path = "sdcard/camera.xmf"; 
			File file = new File(path);

			try 
			{vcm.setCommands(file);} 
			catch (FileNotFoundException e) 
			{e.printStackTrace();} 
			catch (IllegalAccessException e) 
			{e.printStackTrace();}
		}
		else if(SetCommandMethod.BY_FILE_ASSETS == method)
		{
			//please remember to put the camera1.xmf in assets folder
			try 
			{vcm.setCommands(this, "camera1.xmf");} 
			catch (IllegalAccessException e) 
			{e.printStackTrace();} 
			catch (IOException e) 
			{e.printStackTrace();}
		}
		else if(SetCommandMethod.BY_FILE_RAW == method)
		{
			//please remember to put the camera2.xmf in res/raw
			try 
			{vcm.setCommands(this, R.raw.camera2);} 
			catch (NotFoundException e) 
			{e.printStackTrace();} 
			catch (IllegalAccessException e) 
			{e.printStackTrace();}
		}
		
		/*
		 * NOTE: .xmf file for customization is available via the official website
		 * Through the latest customization algorithm on the website,
		 * the performance of VIE can be improved by using the generated file.   
		 */
	}
}

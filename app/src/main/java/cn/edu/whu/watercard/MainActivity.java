package cn.edu.whu.watercard;

import java.io.*;
import java.util.*;
import android.app.*;
import android.content.*;
import android.nfc.*;
import android.nfc.tech.*;
import android.os.*;
import android.view.*;
import android.widget.*;

public class MainActivity extends Activity
{
	private CheckBox checkBox;
	private TextView textViewBalance;
	private EditText editTextBalance;
	private Button buttonWrite;
	private NfcAdapter nfcAdapter;
	private PendingIntent pendingIntent;
	private Tag tag;
	private final byte[] keyA=new byte[]{0x01,0x02,0x03,0x04,0x05,0x06};
	private final int sectorIndex=2;
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		checkBox=findViewById(R.id.checkBox);
		textViewBalance=findViewById(R.id.textViewBalance);
		editTextBalance=findViewById(R.id.editTextBalance);
		buttonWrite=findViewById(R.id.buttonWrite);
		nfcAdapter=NfcAdapter.getDefaultAdapter(this);
		if(nfcAdapter==null)
		{
			Toast.makeText(this,"NFC is not supported by your device!",Toast.LENGTH_LONG).show();
			//finish();
			//return;
		}
		if(!nfcAdapter.isEnabled())
		{
			Toast.makeText(this,"Please enable NFC!",Toast.LENGTH_LONG).show();
			//finish();
			//return;
		}
		buttonWrite.setOnClickListener(new View.OnClickListener()
        {
	        @Override
	        public void onClick(View v)
	        {
	        	if(!checkBox.isChecked())
		        {
			        Toast.makeText(MainActivity.this,"Please agree the ToS!",Toast.LENGTH_LONG).show();
			        return;
		        }
	        	try
		        {
			        int balance=(int)Math.round(Double.parseDouble(editTextBalance.getText().toString())*100);
			        if(balance<0||balance>0x7FFFF)
				        Toast.makeText(MainActivity.this,"Please input a valid value!",Toast.LENGTH_LONG).show();
			        else if(writeTag(tag,MainActivity.this.generateData(balance)))
			        {
				        readTag(tag);
				        Toast.makeText(MainActivity.this,"Successfully Written!",Toast.LENGTH_LONG).show();
			        }
			        else
				        Toast.makeText(MainActivity.this,"Failed to write!",Toast.LENGTH_LONG).show();
		        }
	        	catch(Exception e)
		        {
			        Toast.makeText(MainActivity.this,"Please input a valid value!",Toast.LENGTH_LONG).show();
		        }
	        }
        });
		pendingIntent=PendingIntent.getActivity(this,0,new Intent(this,getClass()),0);
	}

	@Override
	public void onResume()
	{
		super.onResume();
		if(nfcAdapter!=null)
			nfcAdapter.enableForegroundDispatch(this,pendingIntent,null,null);
	}

	@Override
	public void onNewIntent(Intent intent)
	{
		if(!checkBox.isChecked())
		{
			Toast.makeText(this,"Please agree the ToS!",Toast.LENGTH_LONG).show();
			return;
		}
		try
		{
			tag=intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
			String[] techList=tag.getTechList();
			boolean haveMifareUltralight=false;
			for(String tech : techList)
			{
				if(tech.contains("MifareClassic"))
				{
					haveMifareUltralight=true;
					break;
				}
			}
			if(!haveMifareUltralight)
			{
				Toast.makeText(this,"MifareClassic is not supported by your device!",Toast.LENGTH_LONG).show();
				return;
			}
			if(readTag(tag)!=null)
				Toast.makeText(this,"Successfully Read!",Toast.LENGTH_LONG).show();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

	@Override
	public void onPause()
	{
		super.onPause();
		if(nfcAdapter!=null)
			nfcAdapter.disableForegroundDispatch(this);
	}

	private byte[] generateData(int balance)
	{
		byte[] data=new byte[16];
		for(int i=0;i<=3;i++)
		{
			data[i]=(byte)balance;
			balance>>=8;
		}
		data[4]=0x60;data[5]=(byte)0xEA; //changeable:60EA
		//data[7]=0;data[14]=0;//changeable:0000,00C8,0190,0258,0320,0960
		data[10]=(byte)0xA0;data[11]=0x19;//fixed
		for(int i=0;i<=14;i++)
			data[15]+=data[i];
		return data;
	}

	private boolean writeTag(Tag tag,byte[] data)
	{
		MifareClassic mfc=null;
		try
		{
			mfc=MifareClassic.get(tag);
			mfc.connect();
			if(mfc.authenticateSectorWithKeyA(sectorIndex,keyA))
			{
				int blockIndex=mfc.sectorToBlock(sectorIndex);
				mfc.writeBlock(blockIndex,data);
				mfc.writeBlock(++blockIndex,data);
				mfc.close();
				return true;
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			try
			{
				mfc.close();
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
		return false;
	}

	private Integer readTag(Tag tag)
	{
		MifareClassic mfc=MifareClassic.get(tag);
		//Read TAG
		try
		{
			//Enable I/O operations to the tag from this TagTechnology object
			mfc.connect();
			int balance=0;
			//Authenticate a sector with key A
			if(mfc.authenticateSectorWithKeyA(sectorIndex,keyA))
			{
				//Read block
				byte[] data=mfc.readBlock(mfc.sectorToBlock(sectorIndex));
				for(int i=3;i>=0;i--)
				{
					balance<<=8;
					balance|=data[i]&0xFF;
				}
			}
			textViewBalance.setText(String.format(Locale.US,"%.2f",((double)balance)/100));
			return balance;
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			if(mfc!=null)
			{
				try
				{
					mfc.close();
				}
				catch(IOException e)
				{
					e.printStackTrace();
				}
			}
		}
		textViewBalance.setText("");
		return null;
	}
}

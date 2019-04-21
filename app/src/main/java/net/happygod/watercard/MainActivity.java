package net.happygod.watercard;

import java.io.IOException;
import java.util.*;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareClassic;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.*;

public class MainActivity extends Activity
{
	private CheckBox checkBox;
	private TextView textViewBalance;
	private EditText editTextBalance;
	private Button buttonWrite;
	private NfcAdapter mNfcAdapter;
	private PendingIntent mPendingIntent;
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
		mNfcAdapter=NfcAdapter.getDefaultAdapter(this);
		if(mNfcAdapter==null)
		{
			Toast.makeText(this,"设备不支持NFC！",Toast.LENGTH_LONG).show();
			finish();
			return;
		}
		if(!mNfcAdapter.isEnabled())
		{
			Toast.makeText(this,"请在系统设置中先启用NFC功能！",Toast.LENGTH_LONG).show();
			finish();
			return;
		}
		buttonWrite.setOnClickListener(new View.OnClickListener()
       {
           @Override
           public void onClick(View v)
           {
               if(writeTag(tag,MainActivity.this.generateData((int)(Double.parseDouble(editTextBalance.getText().toString())*100))))
               {
               	    readTag(tag);
               }
           }
       });
		mPendingIntent=PendingIntent.getActivity(this,0,new Intent(this,getClass()),0);
	}

	@Override
	public void onResume()
	{
		super.onResume();
		if(mNfcAdapter!=null)
			mNfcAdapter.enableForegroundDispatch(this,mPendingIntent,null,null);
	}

	@Override
	public void onNewIntent(Intent intent)
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
			Toast.makeText(this,"不支持MifareClassic",Toast.LENGTH_LONG).show();
			return;
		}
		Integer data=readTag(tag);
		if(data!=null)
			textViewBalance.setText(String.format(Locale.US,"%.2f",((double)data)/100));
		else
			textViewBalance.setText("");
	}

	@Override
	public void onPause()
	{
		super.onPause();
		if(mNfcAdapter!=null)
			mNfcAdapter.disableForegroundDispatch(this);
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
		MifareClassic mfc=MifareClassic.get(tag);
		try
		{
			mfc.connect();
			if(mfc.authenticateSectorWithKeyA(sectorIndex,keyA))
			{
				int blockIndex=mfc.sectorToBlock(sectorIndex);
				// the last block of the sector is used for KeyA and KeyB cannot be overwritted
				mfc.writeBlock(blockIndex,data);
				mfc.writeBlock(++blockIndex,data);
				mfc.close();
				return true;
			}
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
		finally
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
		return false;
	}

	private Integer readTag(Tag tag)
	{
		MifareClassic mfc=MifareClassic.get(tag);
		//读取TAG
		try
		{
			//Enable I/O operations to the tag from this TagTechnology object.
			mfc.connect();
			//Authenticate a sector with key A.
			int balance=0;
			if(mfc.authenticateSectorWithKeyA(sectorIndex,keyA))
			{
				// 读取扇区中的块
				byte[] data=mfc.readBlock(mfc.sectorToBlock(sectorIndex));
				for(int i=3;i>=0;i--)
				{
					balance<<=8;
					balance|=data[i]&0xFF;
				}
			}
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
		return null;
	}
}

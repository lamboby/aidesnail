package com.itrustoor.boby;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import com.itrustoor.boby.HttpUtil.HttpCallbackListener;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import com.alibaba.sdk.android.oss.OSSService;
import com.alibaba.sdk.android.oss.OSSServiceProvider;
import com.alibaba.sdk.android.oss.callback.SaveCallback;
import com.alibaba.sdk.android.oss.model.AccessControlList;
import com.alibaba.sdk.android.oss.model.ClientConfiguration;
import com.alibaba.sdk.android.oss.model.TokenGenerator;
import com.alibaba.sdk.android.oss.storage.OSSBucket;
import com.alibaba.sdk.android.oss.storage.OSSData;
import com.alibaba.sdk.android.oss.util.OSSToolKit;
import com.alibaba.sdk.android.oss.model.OSSException;
import com.google.zxing.WriterException;
import com.itrustoor.boby.R;
import com.zxing.encoding.EncodingHandler;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemSelectedListener;

public class MainActivity extends Activity{
	Button show;
	Button select;
	Button deselect;
	ListView lv;
	Context mContext;
	MyListAdapter adapter;
/**定义oss存储 参数        */
	static final String accessKey = "fapfVHkNYqnJNNi2"; // 测试代码没有考虑AK/SK的安全性
	static final String screctKey = "B8MR5LlKqqAFRX2G96ctDGgA5O4gon";
	public static OSSService ossService = OSSServiceProvider.getService();
	
	/** 定义WifiManager对象 */
	private WifiManager mainWifi;
	/** 扫描出的网络连接列表 */
	private List<ScanResult> wifiList;
	/** 扫描完毕接收器 */
	private WifiReceiver receiverWifi;
	//二维码存储路径
	private String imgUrl;
	private ImageView imageviewbarcode;
	/* 培训机构唯一编码*/
	private String schoolid;
	private ProgressDialog dialog;
	private EditText tschoolfullname, tschoolshort ,tschooladdress, tschoolpic
	, tschooltel, adminpassword, isp,addonemac,commonpassword;
	private RadioButton rbmain,rbbranch;
	private int once=0;
	
	private Spinner provinceSpinner = null; 	 //省级（省、直辖市）
	private Spinner citySpinner = null;     			//地级市
	private Spinner countySpinner = null;    	//县级（区、县、县级市）
	
	private String province=null;
	private String city=null;
	private String county=null;
	
	private String provincecode=null;
	private String citycode=null;
	private String countycode=null;
	
	private  Button btnsearchwifi ;  
	private  Button btnaddwifi;
	private  Button btncreatebarcode;
	private  Button btnuploadbarcode;
	private  Button btnuploadschool;

	    
	ArrayAdapter<String> provinceAdapter = null;  //省级适配器
	ArrayAdapter<String> cityAdapter = null;    			//地级适配器
	ArrayAdapter<String> countyAdapter = null;   	 //县级适配器
	
	List<Integer> selected = new ArrayList<Integer>();
	private List<Item> items;	
		@Override
		protected void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			setContentView(R.layout.activity_main);
			btnsearchwifi = (Button) findViewById(R.id.btn_searchwifi);  
		    btnaddwifi=(Button)findViewById(R.id.btn_addwifi);
		    btncreatebarcode=(Button)findViewById(R.id.btn_createbarcode);
		    btnuploadbarcode=(Button)findViewById(R.id.btn_uploadBarcode);	    
		    btnuploadschool=(Button)findViewById(R.id.btn_uploadSchool);
			
		
		    
			/** 初始化OSS设置 */
			ossService.setApplicationContext(this.getApplicationContext());
			ossService.setGlobalDefaultTokenGenerator(new TokenGenerator() { // 设置全局默认加签器
				@Override
				public String generateToken(String httpMethod, String md5, String type, String date,
						String ossHeaders, String resource) {

					String content = httpMethod + "\n" + md5 + "\n" + type + "\n" + date + "\n" + ossHeaders
							+ resource;
					return OSSToolKit.generateToken(accessKey, screctKey, content);
				}
			});
			ossService.setGlobalDefaultHostId("oss-cn-shenzhen.aliyuncs.com");
			ossService.setCustomStandardTimeWithEpochSec(System.currentTimeMillis() / 1000);
			ossService.setGlobalDefaultACL(AccessControlList.PUBLIC_READ); // 默认为private

			ClientConfiguration conf = new ClientConfiguration();
			conf.setConnectTimeout(10 * 1000); // 设置全局网络连接超时时间，默认30s
			conf.setSocketTimeout(10 * 1000);	 // 设置全局socket超时时间，默认30s
			conf.setMaxConnections(10); 				// 设置全局最大并发网络链接数, 默认50
			ossService.setClientConfiguration(conf);
			/** 完成OSS初始设置*/
			
			
			//文本输入框不自动获取焦点
			getWindow().setSoftInputMode(
	                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

	        mainWifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
			receiverWifi = new WifiReceiver();
		    items = new ArrayList<Item>();   
		    
		    
		    imageviewbarcode=(ImageView)findViewById(R.id.imgview_barcode);

	        
	        lv = (ListView)findViewById(R.id.list_wifi) ;//WIFI列表
	        tschoolfullname=(EditText)findViewById(R.id.Tschoolfullname);//全称
	        tschoolshort=(EditText)findViewById(R.id.Tschoolshortname);//简称

	        tschoolpic=(EditText)findViewById(R.id.Tschoolpic);//负责人
	        tschooltel=(EditText)findViewById(R.id.Tschoolphone);//电话
	        rbmain=(RadioButton)findViewById(R.id.rbMain);//主机构
	        tschooladdress=(EditText)findViewById(R.id.Taddress);//详细地址
	        rbbranch=(RadioButton)findViewById(R.id.rbBranch);//分支机构
	        adminpassword=(EditText)findViewById(R.id.adminPassword);//管理密码
	        commonpassword=(EditText)findViewById(R.id.commonPassword);//普通密码
	        isp=(EditText)findViewById(R.id.editISP);//建设商
	        
	        
	        provinceSpinner = (Spinner)findViewById(R.id.spin_province); //省列表
	        citySpinner = (Spinner)findViewById(R.id.spin_city);						//市列表
	        countySpinner = (Spinner)findViewById(R.id.spin_county);			//区列表

	  
	        
	       addonemac=(EditText)findViewById(R.id.editMac);//添加一个MAC地址
	        
	        mContext = MainActivity.this;
	      
	    		
	    		
	        //开机初始化省份信息
	         dialog = ProgressDialog.show(MainActivity.this, "", "刷新省份列表,请稍候...");
			 HttpUtil.sendHttpPostRequest("http://121.41.49.137:8080/api/snail/province", "",new HttpCallbackListener(){
				 Message message=new Message();
				 @Override
				 public void onFinish(String response){
						
					dialog.dismiss();	
					message.what=0;
					message.obj=response.toString();
					handler.sendMessage(message);						 
				 }
				 @Override
				 public void onError(Exception e)
				 {
					 Looper.prepare();
					dialog.dismiss();
					 new AlertDialog.Builder(MainActivity.this)
	                  .setTitle("退出")
	                  .setMessage("无法获取省份信息,请检查网络设置!")
	                  .setPositiveButton("确定",new DialogInterface.OnClickListener() {    
	                	  @Override     
	                	  public void onClick(DialogInterface dialog, int which) {      
	                		  finish();    
	                		  }  
	                		  })
	                  . show();	 	  		 
					Looper.loop();				
				 }
					  
			 });
	        
			
	       //上传二维码 
	        btnuploadbarcode.setOnClickListener(new OnClickListener(){
	        	@Override
	        	public void onClick(View v){			    	
	        			saveBitmap();						 
	        	}
	        });
	        
	        //上传学校信息
	        btnuploadschool.setOnClickListener(new OnClickListener(){
	        	@Override
	        	public void onClick(View v){
	        		dialog = ProgressDialog.show(MainActivity.this, "", "正在上传学校数据至云端服务器,请稍候");
	        		String createschool;
	        		createschool="token=1&name="+tschoolfullname.getText().toString().trim()+"&short_name="+tschoolshort.getText().toString().trim()
	        									+"&province_id="+provincecode+"&city_id="+citycode+"&county_id="+countycode+"&macs=";
	        		int inum=items.size();
					String macs="";
					if (inum>0){
						macs="[";
						for (int p=0;p<items.size();p++){
							macs=macs+"\""+items.get(p).address+"\""	;
							if (p<items.size()-1) macs=macs+",";
						}
						macs=macs+"]";
					}
	        		createschool=createschool+macs+"&principal="+tschoolpic.getText().toString().trim()+"&phone="+tschooltel.getText().toString().trim()+"&prcp_pwd="+adminpassword.getText().toString().trim()
	        				                        +"&staff_pwd="+commonpassword.getText().toString().trim()+"&agents_id="+isp.getText().toString().trim()+"&address="+province.toString()+city.toString()+county.toString()+tschooladdress.getText().toString().trim();
	        				                     //   + province+city+county+tschooladdress.getText();
	        	
	   			 HttpUtil.sendHttpPostRequest("http://121.41.49.137:8080/api/snail/createSchool", createschool,new HttpCallbackListener(){
	   				 @Override
	   				 public void onFinish(String response){
	   				 	dialog.dismiss();
	   						Message message=new Message();
	   						message.what=5;
	   						message.obj=response.toString();
	   						handler.sendMessage(message);						 
	   				 }
	   				 @Override
	   				 public void onError(Exception e)
	   				 {
	   					
	   					 Looper.prepare();
	   				  dialog.dismiss();
							
						 new AlertDialog.Builder(MainActivity.this)
		                  .setTitle("退出")
		                  .setMessage("上传学校信息失败,请检查网络设置或联系客服!")
		                  .setPositiveButton("确定",null)
		                  . show();	 	  		 
						Looper.loop();				
	   					 
	   				 }
	   			 });
	        	}
	        });
	        
	        //生成二维码
	        btncreatebarcode.setOnClickListener(new OnClickListener() {
	        	@Override
	        	public void onClick(View v)
	        	{
	        		try {
						String contentString ="<fn>"+tschoolfullname.getText().toString().trim()+"</fn>"+
																	"<sid>"+schoolid+"</sid>";
						int inum=items.size();
						contentString=contentString+"<mnum>"+items.size()+"</mnum>";
						if (inum>0){
							for (int p=0;p<items.size();p++){
								contentString=contentString+"<m"+		p+">"+items.get(p).address+"</m"+p+">"	;
							}
					}												
						if (!contentString.equals("")) {
							//根据字符串生成二维码图片并显示在界面上，第二个参数为图片的大小（）		
							if (items.size()>10)							
							{								
								Toast.makeText(MainActivity.this, "MAC数量不能超过10个", Toast.LENGTH_SHORT).show();
							}
							else
							{
								Bitmap qrCodeBitmap = EncodingHandler.createQRCode(contentString,450);
								imageviewbarcode.setImageBitmap(qrCodeBitmap);								
								//上传二维码可见
								btnuploadbarcode.setVisibility(View.VISIBLE);							
							}
								
						}else {
							Toast.makeText(MainActivity.this, "数据不能为空", Toast.LENGTH_SHORT).show();
						}						
					} catch (WriterException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}	        		
	        	}
	        });
	        
	       
	        //手动添加 MAC地址
	        btnaddwifi.setOnClickListener(new OnClickListener() {  
	            @Override  
	            public void onClick(View v) {  
            	   lv = (ListView)findViewById(R.id.list_wifi) ;	       	       	       	     
	       	  Item item = new Item(); 
	       	   item.name = "None";
  	    		item.address =  addonemac.getText().toString().trim();    
  	    		items.add(item);
  	    		adapter = new MyListAdapter(items);
       	        lv.setAdapter(adapter);    		
            	adapter.notifyDataSetChanged();
            	setListViewHeightBasedOnChildren(lv);  	       	     
	            }  
	        });  
	        
	        //搜索WIFI热点
	        btnsearchwifi.setOnClickListener(new OnClickListener() {  
	            @Override  
	            public void onClick(View v) {  	          
	       	        once=1;	       	        
	     			scanWifi();
	            }  
	        });  
	    }  
		
	        //选择省
	        class SpinnerOnSelectedListener1 implements OnItemSelectedListener{
	    		
	    		public void onItemSelected(AdapterView<?> adapterView, View view, int position,
	    				long id) {
	    			province=((MyListItem) adapterView.getItemAtPosition(position)).getName();
	    			String pcode =((MyListItem) adapterView.getItemAtPosition(position)).getPcode();
	    			provincecode=pcode;
	    			initSpincity(pcode);
	    		
	    		}
	    		public void onNothingSelected(AdapterView<?> adapterView) {
	    			// TODO Auto-generated method stub
	    		}		
	        }
	        //上传二维码路径至云服务器
	        public void saveQR(String imgurl,String schoolid,String desc)
	        {
	        	HttpUtil.sendHttpPostRequest("http://121.41.49.137:8080/api/snail/saveQr", "qr_path="+imgurl+"&sch_id="+schoolid+"&desc="+desc,new HttpCallbackListener(){
				 @Override
				 public void onFinish(String response){
						Message message=new Message();
						message.what=4;
						message.obj=response.toString();
						handler.sendMessage(message);						 
				 }
				 @Override
				 public void onError(Exception e)
				 {
					 Looper.prepare();
					dialog.dismiss();
					 new AlertDialog.Builder(MainActivity.this)
	                  .setTitle("错误")
	                  .setMessage("保存二维码路径至云服务器失败!")
	                  .setPositiveButton("确定",null)
	                  . show();	 	  		 
					 Looper.loop();					 
				 }
			 });
 			
	        	
	        }
	        
	    		//根据选择省查询城市信息
	 		public void initSpincity(String pcode){
	 			 
	 			 HttpUtil.sendHttpPostRequest("http://121.41.49.137:8080/api/snail/city", "pro_id="+pcode,new HttpCallbackListener(){
					 @Override
					 public void onFinish(String response){
							Message message=new Message();
							message.what=1;
							message.obj=response.toString();
							handler.sendMessage(message);						 
					 }
					 @Override
					 public void onError(Exception e)
					 {
						 Looper.prepare();
							//dialog = ProgressDialog.show(MainActivity.this, "", "正在扫描附近WIFI热点,请稍候");
						 new AlertDialog.Builder(MainActivity.this)
		                  .setTitle("退出")
		                  .setMessage("无法获取城市列表,请检查网络设置!")
		                  .setPositiveButton("确定",new DialogInterface.OnClickListener() {    
		                	  @Override     
		                	  public void onClick(DialogInterface dialog, int which) {      
		                		
		                		  }  
		                		  })
		                  . show();	 	  		 
						Looper.loop();
						 
					 }
				 });
	 			
	 		
	  		}
	
	    	    public void initSpincounty(String pcode){
	    	    	 HttpUtil.sendHttpPostRequest("http://121.41.49.137:8080/api/snail/county", "city_id="+pcode,new HttpCallbackListener(){
						 @Override
						 public void onFinish(String response){
								Message message=new Message();
								message.what=2;
								message.obj=response.toString();
								handler.sendMessage(message);						 
						 }
						 @Override
						 public void onError(Exception e)
						 {
							 Looper.prepare();
								//dialog = ProgressDialog.show(MainActivity.this, "", "正在扫描附近WIFI热点,请稍候");
							 new AlertDialog.Builder(MainActivity.this)
			                  .setTitle("退出")
			                  .setMessage("无法获取县区列表,请检查网络设置!")
			                  .setPositiveButton("确定",new DialogInterface.OnClickListener() {    
			                	  @Override     
			                	  public void onClick(DialogInterface dialog, int which) {      
			                		 
			                		  }  
			                		  })
			                  . show();	 	  		 
							Looper.loop();
							 
						 }
					 });	
	    		 	
	    		}
	    	    
	    	    class SpinnerOnSelectedListener2 implements OnItemSelectedListener{
	    			
	    			public void onItemSelected(AdapterView<?> adapterView, View view, int position,
	    					long id) {
	    				city=((MyListItem) adapterView.getItemAtPosition(position)).getName();
	    				String pcode =((MyListItem) adapterView.getItemAtPosition(position)).getPcode();
	    				citycode=pcode;
	    				initSpincounty(pcode);
	    			}

	    			public void onNothingSelected(AdapterView<?> adapterView) {
	    				// TODO Auto-generated method stub
	    			}		
	    		}
	    		
	    		class SpinnerOnSelectedListener3 implements OnItemSelectedListener{
	    			
	    		 public void onItemSelected(AdapterView<?> adapterView, View view, int position,
	    		 long id) {
	    	    	county=((MyListItem) adapterView.getItemAtPosition(position)).getName();
	    		   countycode=((MyListItem) adapterView.getItemAtPosition(position)).getPcode();
	    			}

	    			public void onNothingSelected(AdapterView<?> adapterView) {
	    				// TODO Auto-generated method stub
	    			}		
	    		}
		
		void scanWifi()
		{
		OpenWifi();
		mainWifi.startScan();
			dialog = ProgressDialog.show(MainActivity.this, "", "正在扫描附近WIFI热点,请稍候");
 	
		}	
		
		 protected void saveBitmap() {  
			 OSSService ossService;
			 OSSBucket bucket;
		        try {  
		            // 保存图片到SD卡上  
		        	File appDir = new File(Environment.getExternalStorageDirectory(), "Boohee");
		            if (!appDir.exists()) {
		                appDir.mkdir();
		            }
		            String fileName = System.currentTimeMillis() + ".png";
		            File file = new File(appDir,  tschoolshort.getText().toString()+fileName);
		            /* 保存图片
		           FileOutputStream stream = new FileOutputStream(file);  		            
		           imageviewbarcode.setDrawingCacheEnabled(true);
		            imageviewbarcode.getDrawingCache().compress(CompressFormat.PNG, 100, stream);  
		            imageviewbarcode.setDrawingCacheEnabled(false);		            
		           MediaStore.Images.Media.insertImage(getContentResolver(),   file.getAbsolutePath(), "title", "description");		 
		          */
		            ByteArrayOutputStream baos=new ByteArrayOutputStream();
		            imageviewbarcode.setDrawingCacheEnabled(true);
			        imageviewbarcode.getDrawingCache().compress(CompressFormat.PNG, 100, baos);  
			        imageviewbarcode.setDrawingCacheEnabled(false);		
			        
			        ossService = OSSServiceProvider.getService();
			    	bucket = ossService.getOssBucket("boby-itrustoor"); // 替换为你的bucketName
			    	byte[] dataToUpload = baos.toByteArray();
			    	final OSSData data = ossService.getOssData(bucket, countycode+ tschoolfullname.getText().toString()+"二维码.png");
			    		data.setData(dataToUpload, "image/png");			    	
			    		data.uploadInBackground(new SaveCallback() {			    			
			    			@Override
			    			public void onSuccess(String objectKey) {
			    				 imgUrl=data.getResourceURL();		    				
			    			//	 Looper.prepare();
			    						    				 	
			    			//	 Looper.loop();
			    				 saveQR( imgUrl,schoolid,city+county+tschoolfullname.getText().toString());  		
			    			}

			    			@Override
			    			public void onProgress(String objectKey, int byteCount, int totalSize) {
			    						    			}

			    			@Override
			    			public void onFailure(String objectKey, OSSException ossException) {
			    				Looper.prepare();
			    				dialog.dismiss();
			    				Toast.makeText(MainActivity.this, "异常,OSS返回错误.", 0).show();  
			    				Looper.loop();			    				 	
			    			}
			    		});	           		              

		        } catch (Exception e) {  
		            Toast.makeText(MainActivity.this, "异常,OSS返回错误.", 0).show();  
		            e.printStackTrace();  
		        }  

		    }  	
	   //自定义ListView适配器
    class MyListAdapter extends BaseAdapter{

        LayoutInflater inflater;
    	public List<Item> items;
        public MyListAdapter(List<Item> items){
        	this.items = items;    		
        	inflater = LayoutInflater.from(mContext);			
        }
        @Override
		public int getCount() {
			// 返回值控制该Adapter将会显示多少个列表项
	        return items == null ? 0 : items.size();
		}

		@Override
		public Object getItem(int position) {
			// 返回值决定第position处的列表项的内容
			return items.get(position);
		}

		@Override
		public long getItemId(int position) {
			// 返回值决定第position处的列表项的ID
			return position;
		}
		 
		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {
		    ViewHolder holder = null;
			Item item = items.get(position);
		    if (convertView == null) {

				convertView = inflater.inflate(R.layout.listitem, null);				
				holder = new ViewHolder();
				holder.btnDel = (Button)convertView.findViewById(R.id.btn_deletewifi);
				holder.tvName = (TextView)convertView.findViewById(R.id.tvName);
				holder.tvAddress = (TextView)convertView.findViewById(R.id.tvAddress);
			    holder.tvName.setText(item.name);
			  	holder.tvAddress.setText(item.address);				
			    convertView.setTag(holder);			    
			}else{			
				holder = (ViewHolder)convertView.getTag();
                holder.tvName.setText(item.name);
                holder.tvAddress.setText(item.address);              
			}
		   holder.btnDel.setOnClickListener(new OnClickListener(){

		@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					  //删除list中的数据
                    items.remove(position);
                    //通知列表数据修改
                   adapter.notifyDataSetChanged();
                   setListViewHeightBasedOnChildren(lv);
				}		    	
		    });
		
		   return convertView;  
		}    	
    }
    
    //动态改变列表高度
    public void setListViewHeightBasedOnChildren(ListView listView) {  
    	  
    	  ListAdapter listAdapter = listView.getAdapter();      	  
    	  if (listAdapter == null) {  
    	   return;  
    	  }      	  
    	  int totalHeight = 0;      	  
    	  for (int i = 0; i < listAdapter.getCount(); i++) {  
    	   View listItem = listAdapter.getView(i, null, listView);  
    	   listItem.measure(0, 0);  
    	   totalHeight += listItem.getMeasuredHeight();  
    	  }  
    	  
    	  ViewGroup.LayoutParams params = listView.getLayoutParams();      	  
    	  params.height = totalHeight  
    	    + (listView.getDividerHeight() * (listAdapter.getCount() - 1));      	  
    	  ((MarginLayoutParams) params).setMargins(10, 10, 10, 10); // 可删除      	  
    	  listView.setLayoutParams(params);  
    	 }  
    
    static class ViewHolder{
      	public TextView tvName;
    	public TextView tvAddress;
    	public Button btnDel;
    	
    }
    
    class Item {
        private String name;
        private String address;
    }   
    
    /**
	 * 打开WIFI
	 */
	public void OpenWifi()
	{
		if (!mainWifi.isWifiEnabled())
		{
			mainWifi.setWifiEnabled(true);
		}
	}
	protected void onPause()
	{
		super.onPause();
		unregisterReceiver(receiverWifi);// 注销广播
	}
	
	//接收程序消息
	 private Handler handler=new Handler(){
			public void handleMessage(Message msg){		
	    
				String response=(String)msg.obj;			
				List<MyListItem> list = new ArrayList<MyListItem>();					
				JSONObject resObject;
				try {
				
					resObject = new JSONObject(response);				
					if (resObject.getInt("Code")==0){
						JSONArray jsonArray;
						
						switch( msg.what){
						case 0://选择省		
							 jsonArray= new JSONObject(response).getJSONArray("Data");   
			    		    for (int i=0;i<jsonArray.length();i++){	
			    		    		JSONObject jsonObject=jsonArray.getJSONObject(i);
			    		    	   String id=jsonObject.getString("id");
			    		    	   String name=jsonObject.getString("name");
			    		    	   MyListItem myListItem=new MyListItem();
			    		    	   myListItem.setName(name);
			    		    	   myListItem.setPcode(id);
			    		    	   list.add(myListItem);
			    		     }		    	      
			    		    MyAdapter myAdapter = new MyAdapter(MainActivity.this,list);
			    		    provinceSpinner.setAdapter(myAdapter);
			    		    provinceSpinner.setOnItemSelectedListener(new SpinnerOnSelectedListener1());
			    		    break;
						case 1:     
							 jsonArray= new JSONObject(response).getJSONArray("Data");   
			    		    for (int i=0;i<jsonArray.length();i++){	
			    		    	JSONObject jsonObject=jsonArray.getJSONObject(i);
			    		    	String id=jsonObject.getString("id");
			    		    	String name=jsonObject.getString("name");
			    		    	
			    		    	MyListItem myListItem=new MyListItem();
			    		    	myListItem.setName(name);
			    		    	myListItem.setPcode(id);
			    		    	list.add(myListItem);
			    		     }		    
			    		    MyAdapter myAdapter1 = new MyAdapter(MainActivity.this,list);
					 		citySpinner.setAdapter(myAdapter1);
				  		 	citySpinner.setOnItemSelectedListener(new SpinnerOnSelectedListener2());
				  		 	break;
						case 2:
						
							 jsonArray= new JSONObject(response).getJSONArray("Data");   
			    		    for (int i=0;i<jsonArray.length();i++){	
			    		    	JSONObject jsonObject=jsonArray.getJSONObject(i);
			    		    	String id=jsonObject.getString("id");
			    		    	String name=jsonObject.getString("name");
			    		    	MyListItem myListItem=new MyListItem();
			    		    	myListItem.setName(name);
			    		    	myListItem.setPcode(id);
			    		    	list.add(myListItem);
			    		     }			    	  
			    		    MyAdapter myAdapter2= new MyAdapter(MainActivity.this,list);
			    		    countySpinner.setAdapter(myAdapter2);
			    		    countySpinner.setOnItemSelectedListener(new SpinnerOnSelectedListener3());
			    		    break;
						case 4:							 
							Toast.makeText(MainActivity.this, "二维码成功上传至云服务器", 0).show();
							break;
						case 5:
							 jsonArray= new JSONObject(response).getJSONArray("Data");   
							 JSONObject jsonObject=jsonArray.getJSONObject(0);
							 schoolid=jsonObject.getString("id");			    		    	
							 Toast.makeText(MainActivity.this, "学校信息上传完成", 0).show();   
							 btncreatebarcode.setVisibility(View.VISIBLE);			    		   
							 break;					
					default:
						break;
						}
				}
				else
				{
					 new AlertDialog.Builder(MainActivity.this)
	                  .setTitle("服务器返回码:"+resObject.getString("Code"))
	                  .setMessage(resObject.getString("Msg"))
	                  .setPositiveButton("确定",null)
	                  . show();	 	  		 //如果返回码不等于0
				}
	}
	catch (Exception e) {  
	}  	 	
				}
		
	 }; 
	 
	 //活动运行注册WIFI广播
	protected void onResume()
	{
		super.onResume();		
		 IntentFilter filter = new IntentFilter();  
		 filter.addAction( WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
		 registerReceiver(receiverWifi,filter);// 注册广播
	}
    
	//接收WIFI广播
	class WifiReceiver extends BroadcastReceiver
	{
		public String[] getString(List<ScanResult> wifiList)
		{
			ArrayList<String> listStr = new ArrayList<String>();
			for (int i = 0; i < wifiList.size(); i++)
			{
				listStr.add(wifiList.get(i).toString());
			}
			return listStr.toArray(new String[0]);
		}
		
		public void onReceive(Context context, Intent intent)
		{
			if (once==1) { 
			ScanResult result = null;
			items.clear();
				wifiList = mainWifi.getScanResults();
				 dialog.dismiss();
				Toast.makeText(context, "扫描到 "+wifiList.size()+" 个热点信息.", Toast.LENGTH_LONG).show();
				for (int i = 0; i <wifiList.size(); i++) {
					 result = wifiList.get(i);	
					 Item item = new Item();
					 item.name=result.SSID;
					 item.address=result.BSSID;
						items.add(item);
					 }
				 adapter = new MyListAdapter(items);
				lv.setAdapter(adapter);    		
				setListViewHeightBasedOnChildren(lv);
				once=0;
			}
					
        }  
    }  
}


package org.stagex.danmaku.activity;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.net.ftp.FTPClient;
import org.keke.player.R;
import org.stagex.danmaku.adapter.ChannelAdapter;
import org.stagex.danmaku.adapter.ChannelInfo;
import org.stagex.danmaku.util.ParseUtil;

import com.nmbb.oplayer.ui.MainActivity;

import android.app.AlertDialog;
import android.app.TabActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TabHost;
import android.widget.Toast;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TextView;

@SuppressWarnings("deprecation")
public class ChannelTabActivity extends TabActivity implements
		OnTabChangeListener {

	private static final String LOGTAG = "ChannelTabActivity";
	
	List<ChannelInfo> allinfos = null;

	List<ChannelInfo> yangshi_infos = null;
	List<ChannelInfo> weishi_infos = null;
	List<ChannelInfo> difang_infos = null;
	List<ChannelInfo> tiyu_infos = null;

	private ListView yang_shi_list;
	private ListView wei_shi_list;
	private ListView di_fang_list;
	private ListView ti_yu_list;

	private TabHost myTabhost;

	TextView view0, view1, view2, view3;

	/* 顶部标题栏的控件 */
	private Button button_home;
	private Button button_back;
	private Button button_refresh;
	
	/* 列表更新成功标志 */
	private SharedPreferences sharedPreferences;
	private Editor editor;
	private boolean isTVListSuc;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.tab_channel);

		/* 顶部标题栏的控件 */
		button_home = (Button) findViewById(R.id.home_btn);
		button_back = (Button) findViewById(R.id.back_btn);
		button_refresh = (Button) findViewById(R.id.refresh_btn);
		
		//记录更新成功还是失败
	    sharedPreferences = getSharedPreferences("keke_player", MODE_PRIVATE);
	    editor = sharedPreferences.edit();
		
		setListensers();
		
		myTabhost = this.getTabHost();
		myTabhost.setup();

		myTabhost.setBackgroundDrawable(this.getResources().getDrawable(
				R.drawable.bg_home));

		/* 设置每一个台类别的Tab */
		RelativeLayout tab0 = (RelativeLayout) LayoutInflater.from(this)
				.inflate(R.layout.tab_host_ctx, null);
		view0 = (TextView) tab0.findViewById(R.id.tab_label);
		view0.setText("央视");
		myTabhost.addTab(myTabhost.newTabSpec("One")// make a new Tab
				.setIndicator(tab0)
				// set the Title and Icon
				.setContent(R.id.yang_shi_tab));
		// set the layout

		RelativeLayout tab1 = (RelativeLayout) LayoutInflater.from(this)
				.inflate(R.layout.tab_host_ctx, null);
		view1 = (TextView) tab1.findViewById(R.id.tab_label);
		view1.setText("卫视");
		myTabhost.addTab(myTabhost.newTabSpec("Two")// make a new Tab
				.setIndicator(tab1)
				// set the Title and Icon
				.setContent(R.id.wei_shi_tab));
		// set the layout

		RelativeLayout tab2 = (RelativeLayout) LayoutInflater.from(this)
				.inflate(R.layout.tab_host_ctx, null);
		view2 = (TextView) tab2.findViewById(R.id.tab_label);
		view2.setText("地方");
		myTabhost.addTab(myTabhost.newTabSpec("Three")// make a new Tab
				.setIndicator(tab2)
				// set the Title and Icon
				.setContent(R.id.di_fang_tab));
		// set the layout

		RelativeLayout tab3 = (RelativeLayout) LayoutInflater.from(this)
				.inflate(R.layout.tab_host_ctx, null);
		view3 = (TextView) tab3.findViewById(R.id.tab_label);
		view3.setText("体育");
		myTabhost.addTab(myTabhost.newTabSpec("Four")// make a new Tab
				.setIndicator(tab3)
				// set the Title and Icon
				.setContent(R.id.ti_yu_tab));
		// set the layout

		/* 设置Tab的监听事件 */
		myTabhost.setOnTabChangedListener(this);

		/* 解析所有的channel list 区分是采用默认列表还是服务器列表 */
		isTVListSuc = sharedPreferences.getBoolean("isTVListSuc", false);
		allinfos = ParseUtil.parse(this,  isTVListSuc);
		if (isTVListSuc)
			Log.d(LOGTAG, "采用服务器更新后的播放列表");
		else
			Log.d(LOGTAG, "采用本地备份的播放列表");

		/* 获得各个台类别的list */
		yang_shi_list = (ListView) findViewById(R.id.yang_shi_tab);
		// 防止滑动黑屏
		yang_shi_list.setCacheColorHint(Color.TRANSPARENT);
		wei_shi_list = (ListView) findViewById(R.id.wei_shi_tab);
		// 防止滑动黑屏
		wei_shi_list.setCacheColorHint(Color.TRANSPARENT);
		di_fang_list = (ListView) findViewById(R.id.di_fang_tab);
		// 防止滑动黑屏
		di_fang_list.setCacheColorHint(Color.TRANSPARENT);
		ti_yu_list = (ListView) findViewById(R.id.ti_yu_tab);
		// 防止滑动黑屏
		ti_yu_list.setCacheColorHint(Color.TRANSPARENT);

		// 默认显示第一个标签
		view0.setTextSize(25);
		view1.setTextSize(15);
		view2.setTextSize(15);
		view3.setTextSize(15);
		setYangshiView();
		setWeishiView();
		setDifangView();
		setTiyuView();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return false;
	}

	@Override
	public void onTabChanged(String tagString) {
		// TODO Auto-generated method stub
		if (tagString.equals("One")) {
			view0.setTextSize(25);
			view1.setTextSize(15);
			view2.setTextSize(15);
			view3.setTextSize(15);
			// setYangshiView();
		}
		if (tagString.equals("Two")) {
			view0.setTextSize(15);
			view1.setTextSize(25);
			view2.setTextSize(15);
			view3.setTextSize(15);
			// setWeishiView();
		}
		if (tagString.equals("Three")) {
			view0.setTextSize(15);
			view1.setTextSize(15);
			view2.setTextSize(25);
			view3.setTextSize(15);
			// setDifangView();
		}

		if (tagString.equals("Four")) {
			view0.setTextSize(15);
			view1.setTextSize(15);
			view2.setTextSize(15);
			view3.setTextSize(25);
			// setTiyuView();
		}
	}

	/*
	 * 设置央视台源的channel list
	 */
	private void setYangshiView() {
		yangshi_infos = getYangShi(allinfos);
		ChannelAdapter adapter = new ChannelAdapter(this, yangshi_infos);
		yang_shi_list.setAdapter(adapter);
		yang_shi_list.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				// TODO Auto-generated method stub
				ChannelInfo info = (ChannelInfo) yang_shi_list
						.getItemAtPosition(arg2);
				Log.d("ChannelInfo",
						"name = " + info.getName() + "[" + info.getUrl() + "]");

//				startLiveMedia(info.getUrl(), info.getName());
				showAllSource(info.getAllUrl(), info.getName());
			}
		});
		yang_shi_list.setOnScrollListener(new OnScrollListener() {

			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onScroll(AbsListView view, int firstVisibleItem,
					int visibleItemCount, int totalItemCount) {
				// TODO Auto-generated method stub

			}
		});
	}

	/*
	 * 设置卫视台源的channel list
	 */
	private void setWeishiView() {
		weishi_infos = getWeiShi(allinfos);
		ChannelAdapter adapter = new ChannelAdapter(this, weishi_infos);
		wei_shi_list.setAdapter(adapter);
		wei_shi_list.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				// TODO Auto-generated method stub
				ChannelInfo info = (ChannelInfo) wei_shi_list
						.getItemAtPosition(arg2);
				Log.d("ChannelInfo",
						"name = " + info.getName() + "[" + info.getUrl() + "]");

//				startLiveMedia(info.getUrl(), info.getName());
				showAllSource(info.getAllUrl(), info.getName());
			}
		});

		wei_shi_list.setOnScrollListener(new OnScrollListener() {

			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onScroll(AbsListView view, int firstVisibleItem,
					int visibleItemCount, int totalItemCount) {
				// TODO Auto-generated method stub

			}
		});
	}

	/*
	 * 设置地方台源的channel list
	 */
	private void setDifangView() {
		difang_infos = getDiFang(allinfos);
		ChannelAdapter adapter = new ChannelAdapter(this, difang_infos);
		di_fang_list.setAdapter(adapter);
		di_fang_list.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				// TODO Auto-generated method stub
				ChannelInfo info = (ChannelInfo) di_fang_list
						.getItemAtPosition(arg2);
				Log.d("ChannelInfo",
						"name = " + info.getName() + "[" + info.getUrl() + "]");

//				startLiveMedia(info.getUrl(), info.getName());
				showAllSource(info.getAllUrl(), info.getName());
			}
		});

		di_fang_list.setOnScrollListener(new OnScrollListener() {

			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onScroll(AbsListView view, int firstVisibleItem,
					int visibleItemCount, int totalItemCount) {
				// TODO Auto-generated method stub

			}
		});
	}

	/*
	 * 设置体育台源的channel list
	 */
	private void setTiyuView() {
		tiyu_infos = getTiYu(allinfos);
		ChannelAdapter adapter = new ChannelAdapter(this, tiyu_infos);
		ti_yu_list.setAdapter(adapter);
		ti_yu_list.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				// TODO Auto-generated method stub
				ChannelInfo info = (ChannelInfo) ti_yu_list
						.getItemAtPosition(arg2);
				Log.d("ChannelInfo",
						"name = " + info.getName() + "[" + info.getUrl() + "]");

//				startLiveMedia(info.getUrl(), info.getName());
				showAllSource(info.getAllUrl(), info.getName());
			}
		});

		ti_yu_list.setOnScrollListener(new OnScrollListener() {

			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onScroll(AbsListView view, int firstVisibleItem,
					int visibleItemCount, int totalItemCount) {
				// TODO Auto-generated method stub

			}
		});
	}
	
	/**
	 * 	显示所有的台源
	 */
	private void showAllSource(ArrayList<String> all_url, String name) {
		Intent intent = new Intent(ChannelTabActivity.this,
				ChannelSourceActivity.class);
		intent.putExtra("all_url", all_url);
		intent.putExtra("channel_name", name);
		startActivity(intent);
	}
	
//	private void startLiveMedia(String liveUrl, String name) {
//		Intent intent = new Intent(ChannelTabActivity.this,
//				PlayerActivity.class);
//		ArrayList<String> playlist = new ArrayList<String>();
//		playlist.add(liveUrl);
//		intent.putExtra("selected", 0);
//		intent.putExtra("playlist", playlist);
//		intent.putExtra("title", name);
//		startActivity(intent);
//	}

	/*
	 * 从所有的台源中解析出央视的台源
	 */
	private List<ChannelInfo> getYangShi(List<ChannelInfo> all) {
		List<ChannelInfo> info = new ArrayList<ChannelInfo>();

		for (int i = 0; i < all.size(); i++) {
			ChannelInfo cinfo = all.get(i);
			if (cinfo.getTypes().equals("1") || cinfo.getTypes().equals("1|4")) {
				info.add(cinfo);
			}
		}
		return info;
	}

	/*
	 * 从所有的台源中解析出央视的台源
	 */
	private List<ChannelInfo> getWeiShi(List<ChannelInfo> all) {
		List<ChannelInfo> info = new ArrayList<ChannelInfo>();

		for (int i = 0; i < all.size(); i++) {
			ChannelInfo cinfo = all.get(i);
			if (cinfo.getTypes().equals("2") || cinfo.getTypes().equals("2|4")) {
				info.add(cinfo);
			}
		}
		return info;
	}

	/*
	 * 从所有的台源中解析出央视的台源
	 */
	private List<ChannelInfo> getDiFang(List<ChannelInfo> all) {
		List<ChannelInfo> info = new ArrayList<ChannelInfo>();

		for (int i = 0; i < all.size(); i++) {
			ChannelInfo cinfo = all.get(i);
			if (cinfo.getTypes().equals("3") || cinfo.getTypes().equals("3|4")) {
				info.add(cinfo);
			}
		}
		return info;
	}

	/*
	 * 从所有的台源中解析出央视的台源
	 */
	private List<ChannelInfo> getTiYu(List<ChannelInfo> all) {
		List<ChannelInfo> info = new ArrayList<ChannelInfo>();

		for (int i = 0; i < all.size(); i++) {
			ChannelInfo cinfo = all.get(i);
			if (cinfo.getTypes().equals("4") || cinfo.getTypes().equals("1|4")
					|| cinfo.getTypes().equals("2|4")
					|| cinfo.getTypes().equals("3|4")) {
				info.add(cinfo);
			}
		}
		return info;
	}

	// Listen for button clicks
	private void setListensers() {
		button_home.setOnClickListener(goListener);
		button_back.setOnClickListener(goListener);
		button_refresh.setOnClickListener(goListener);
	}
	
	private Button.OnClickListener goListener = new Button.OnClickListener() {
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.home_btn:
				//退回主界面(homeActivity)
				finish();
				Intent intent = new Intent(ChannelTabActivity.this,
						HomeActivity.class);
				startActivity(intent);
				break;
			case R.id.back_btn:
				//回到上一个界面(Activity)
				finish();
				break;
			case R.id.refresh_btn:
				//TODO 到远程服务器下载直播电视播放列表
				tvPlaylistDownload();
				
				isTVListSuc = sharedPreferences.getBoolean("isTVListSuc", false);
				
				if (isTVListSuc) {
				//弹出加载【成功】对话框
				new AlertDialog.Builder(ChannelTabActivity.this)
			    .setTitle("更新成功")
			    .setMessage("直播地址更新完毕!\n请点击【确定】\n重新进入当前界面")
			    .setNegativeButton("确定", new DialogInterface.OnClickListener() {
			        @Override
			        public void onClick(DialogInterface dialog, int which) {
			            //do nothing - it will close on its own
			        	finish();
			        }
			     })
			   .show();
				} else {
					//弹出加载【失败】对话框
					new AlertDialog.Builder(ChannelTabActivity.this)
				    .setTitle("更新失败")
				    .setMessage("直播地址更新失败!\n采用本地备份的列表")
				    .setNegativeButton("确定", new DialogInterface.OnClickListener() {
				        @Override
				        public void onClick(DialogInterface dialog, int which) {
				            //do nothing - it will close on its own
				        }
				     })
				   .show();
				}
				
				break;
			default:
				Log.d(LOGTAG, "not supported btn id");
			}
		}
	};
	
    /**
     * FTP下载单个文件测试
     */
    private void tvPlaylistDownload() {
        FTPClient ftpClient = new FTPClient();
        FileOutputStream fos = null;
        
        //假设更新列表成功
        editor.putBoolean("isTVListSuc", true);
        editor.commit();
        
        try {
            ftpClient.connect("ftp92147.host217.web519.com");
            ftpClient.login("ftp92147", "950288@kk");
            
            //此处不需要Data前面的"/"
            String remoteFileName = "Data/channel_list_cn.list.api2";
            //此处要注意必须加上channel_list_cn.list.api2前面的"/"
            fos = new FileOutputStream(Environment.getExternalStorageDirectory().getPath() + "/.channel_list_cn.list.api2");

            ftpClient.setBufferSize(1024);
            //设置文件类型（二进制）
            ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE);
            ftpClient.retrieveFile(remoteFileName, fos);
        } catch (IOException e) {
            e.printStackTrace();
          //更新列表失败
            editor.putBoolean("isTVListSuc", false);  
            editor.commit();
//            throw new RuntimeException("FTP客户端出错！", e);
        } finally {
            try {
            	if (fos != null) {
            		//TODO 需要对文件的合法性作一定的测试，例如大小
            		fos.close();
            	}
			} catch (IOException e) {
				e.printStackTrace();
				//更新列表失败
				editor.putBoolean("isTVListSuc", false);  
                editor.commit();
//				throw new RuntimeException("关闭文件发生异常！", e);
			}
            try {
                ftpClient.disconnect();
            } catch (IOException e) {
                e.printStackTrace();
              //更新列表失败
                editor.putBoolean("isTVListSuc", false);  
                editor.commit();
//                throw new RuntimeException("关闭FTP连接发生异常！", e);
            }
        }
    }
	
}

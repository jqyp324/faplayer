package org.stagex.danmaku.activity;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.keke.player.R;
import org.stagex.danmaku.adapter.ChannelAdapter;
import org.stagex.danmaku.adapter.ChannelInfo;
import org.stagex.danmaku.util.ParseUtil;
import org.stagex.danmaku.util.ThreadPool;

import android.app.AlertDialog;
import android.app.TabActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TabHost;
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
	List<ChannelInfo> yule_infos = null;
	List<ChannelInfo> qita_infos = null;

	private ListView yang_shi_list;
	private ListView wei_shi_list;
	private ListView di_fang_list;
	private ListView ti_yu_list;
	private ListView yu_le_list;
	private ListView qi_ta_list;

	private TabHost myTabhost;

	TextView view0, view1, view2, view3, view4, view5;

	/* 顶部标题栏的控件 */
	private ImageView button_home;
	private TextView button_back;
	private ImageView button_refresh;

	/* 列表更新成功标志 */
	private SharedPreferences sharedPreferences;
	private Editor editor;
	private boolean isTVListSuc;
	/* 如果没有备份过服务器地址，则加载本地初始的地址 */
	private boolean hasBackup;

	/* 旋转图标 */
	private Animation operatingAnim;
	private LinearInterpolator lin;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.tab_channel);

		/* 顶部标题栏的控件 */
		button_home = (ImageView) findViewById(R.id.home_btn);
		button_back = (TextView) findViewById(R.id.back_btn);
		button_refresh = (ImageView) findViewById(R.id.refresh_btn);
		/* 旋转图标 */
		operatingAnim = AnimationUtils.loadAnimation(this, R.anim.tip);
		lin = new LinearInterpolator();
		operatingAnim.setInterpolator(lin);

		// 记录更新成功还是失败
		sharedPreferences = getSharedPreferences("keke_player", MODE_PRIVATE);
		editor = sharedPreferences.edit();

		setListensers();
		initializeEvents();

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
		
		RelativeLayout tab4 = (RelativeLayout) LayoutInflater.from(this)
				.inflate(R.layout.tab_host_ctx, null);
		view4 = (TextView) tab4.findViewById(R.id.tab_label);
		view4.setText("娱乐");
		myTabhost.addTab(myTabhost.newTabSpec("Five")// make a new Tab
				.setIndicator(tab4)
				// set the Title and Icon
				.setContent(R.id.yu_le_tab));
		// set the layout
		
		RelativeLayout tab5 = (RelativeLayout) LayoutInflater.from(this)
				.inflate(R.layout.tab_host_ctx, null);
		view5 = (TextView) tab5.findViewById(R.id.tab_label);
		view5.setText("其他");
		myTabhost.addTab(myTabhost.newTabSpec("Six")// make a new Tab
				.setIndicator(tab5)
				// set the Title and Icon
				.setContent(R.id.qi_ta_tab));
		// set the layout

		/* 设置Tab的监听事件 */
		myTabhost.setOnTabChangedListener(this);

		/* 解析所有的channel list 区分是采用默认列表还是服务器列表 */
		hasBackup = sharedPreferences.getBoolean("hasBackup", false);
		allinfos = ParseUtil.parse(this, hasBackup);
		if (hasBackup)
			Log.d(LOGTAG, "采用服务器更新后的播放列表");
		else {
			// 如果没有备份过地址，第一次自动加载服务器地址
			startRefreshList();
			Log.d(LOGTAG, "采用初始的播放列表");
		}

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
		
		yu_le_list = (ListView) findViewById(R.id.yu_le_tab);
		// 防止滑动黑屏
		yu_le_list.setCacheColorHint(Color.TRANSPARENT);
		
		qi_ta_list = (ListView) findViewById(R.id.qi_ta_tab);
		// 防止滑动黑屏
		qi_ta_list.setCacheColorHint(Color.TRANSPARENT);

		// 默认显示第一个标签
		view0.setTextSize(25);
		view1.setTextSize(15);
		view2.setTextSize(15);
		view3.setTextSize(15);
		view4.setTextSize(15);
		view5.setTextSize(15);
		setYangshiView();
		setWeishiView();
		setDifangView();
		setTiyuView();
		setYuleView();
		setQitaView();
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
			view4.setTextSize(15);
			view5.setTextSize(15);
		}
		if (tagString.equals("Two")) {
			view0.setTextSize(15);
			view1.setTextSize(25);
			view2.setTextSize(15);
			view3.setTextSize(15);
			view4.setTextSize(15);
			view5.setTextSize(15);
		}
		if (tagString.equals("Three")) {
			view0.setTextSize(15);
			view1.setTextSize(15);
			view2.setTextSize(25);
			view3.setTextSize(15);
			view4.setTextSize(15);
			view5.setTextSize(15);
		}
		if (tagString.equals("Four")) {
			view0.setTextSize(15);
			view1.setTextSize(15);
			view2.setTextSize(15);
			view3.setTextSize(25);
			view4.setTextSize(15);
			view5.setTextSize(15);
		}
		if (tagString.equals("Five")) {
			view0.setTextSize(15);
			view1.setTextSize(15);
			view2.setTextSize(15);
			view3.setTextSize(15);
			view4.setTextSize(25);
			view5.setTextSize(15);
		}
		if (tagString.equals("Six")) {
			view0.setTextSize(15);
			view1.setTextSize(15);
			view2.setTextSize(15);
			view3.setTextSize(15);
			view4.setTextSize(15);
			view5.setTextSize(25);
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

				// startLiveMedia(info.getUrl(), info.getName());
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

				// startLiveMedia(info.getUrl(), info.getName());
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

				// startLiveMedia(info.getUrl(), info.getName());
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

				// startLiveMedia(info.getUrl(), info.getName());
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
	
	/*
	 * 设置娱乐台源的channel list
	 */
	private void setYuleView() {
		yule_infos = getYule(allinfos);
		ChannelAdapter adapter = new ChannelAdapter(this, yule_infos);
		yu_le_list.setAdapter(adapter);
		yu_le_list.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				// TODO Auto-generated method stub
				ChannelInfo info = (ChannelInfo) yu_le_list
						.getItemAtPosition(arg2);
				Log.d("ChannelInfo",
						"name = " + info.getName() + "[" + info.getUrl() + "]");

				// startLiveMedia(info.getUrl(), info.getName());
				showAllSource(info.getAllUrl(), info.getName());
			}
		});

		yu_le_list.setOnScrollListener(new OnScrollListener() {

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
	 * 设置其他未分类台源的channel list
	 */
	private void setQitaView() {
		qita_infos = getQita(allinfos);
		ChannelAdapter adapter = new ChannelAdapter(this, qita_infos);
		qi_ta_list.setAdapter(adapter);
		qi_ta_list.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				// TODO Auto-generated method stub
				ChannelInfo info = (ChannelInfo) qi_ta_list
						.getItemAtPosition(arg2);
				Log.d("ChannelInfo",
						"name = " + info.getName() + "[" + info.getUrl() + "]");

				// startLiveMedia(info.getUrl(), info.getName());
				showAllSource(info.getAllUrl(), info.getName());
			}
		});

		qi_ta_list.setOnScrollListener(new OnScrollListener() {

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
	 * 显示所有的台源
	 */
	private void showAllSource(ArrayList<String> all_url, String name) {
		Intent intent = new Intent(ChannelTabActivity.this,
				ChannelSourceActivity.class);
		intent.putExtra("all_url", all_url);
		intent.putExtra("channel_name", name);
		startActivity(intent);
	}

	/*
	 * 从所有的台源中解析出央视的台源 ==> id = 1
	 */
	private List<ChannelInfo> getYangShi(List<ChannelInfo> all) {
		List<ChannelInfo> info = new ArrayList<ChannelInfo>();

		for (int i = 0; i < all.size(); i++) {
			ChannelInfo cinfo = all.get(i);
			if (cinfo.getTypes().equals("1") || cinfo.getTypes().equals("1|4") || cinfo.getTypes().equals("1|5"))  {
				info.add(cinfo);
			}
		}
		return info;
	}

	/*
	 * 从所有的台源中解析出卫视的台源 ==> id = 2
	 */
	private List<ChannelInfo> getWeiShi(List<ChannelInfo> all) {
		List<ChannelInfo> info = new ArrayList<ChannelInfo>();

		for (int i = 0; i < all.size(); i++) {
			ChannelInfo cinfo = all.get(i);
			if (cinfo.getTypes().equals("2") || cinfo.getTypes().equals("2|4") || cinfo.getTypes().equals("2|5")) {
				info.add(cinfo);
			}
		}
		return info;
	}

	/*
	 * 从所有的台源中解析出地方的台源 ==> id = 3
	 */
	private List<ChannelInfo> getDiFang(List<ChannelInfo> all) {
		List<ChannelInfo> info = new ArrayList<ChannelInfo>();

		for (int i = 0; i < all.size(); i++) {
			ChannelInfo cinfo = all.get(i);
			if (cinfo.getTypes().equals("3") || cinfo.getTypes().equals("3|4") || cinfo.getTypes().equals("3|5")) {
				info.add(cinfo);
			}
		}
		return info;
	}

	/*
	 * 从所有的台源中解析出体育的台源 ==> id = 4
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
	
	/*
	 * 从所有的台源中解析出娱乐的台源 ==>id = 5
	 */
	private List<ChannelInfo> getYule(List<ChannelInfo> all) {
		List<ChannelInfo> info = new ArrayList<ChannelInfo>();

		for (int i = 0; i < all.size(); i++) {
			ChannelInfo cinfo = all.get(i);
			if (cinfo.getTypes().equals("5") || cinfo.getTypes().equals("1|5")
					|| cinfo.getTypes().equals("2|5")
					|| cinfo.getTypes().equals("3|5")) {
				info.add(cinfo);
			}
		}
		return info;
	}

	/*
	 * 从所有的台源中解析出其他未分类的台源 ==>id = 6
	 */
	private List<ChannelInfo> getQita(List<ChannelInfo> all) {
		List<ChannelInfo> info = new ArrayList<ChannelInfo>();

		for (int i = 0; i < all.size(); i++) {
			ChannelInfo cinfo = all.get(i);
			if (cinfo.getTypes().equals("6")) {
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
				// 退回主界面(homeActivity)
				finish();
				Intent intent = new Intent(ChannelTabActivity.this,
						HomeActivity.class);
				startActivity(intent);
				break;
			case R.id.back_btn:
				// 回到上一个界面(Activity)
				finish();
				break;
			case R.id.refresh_btn:
				// 更新服务器地址
				startRefreshList();
				break;
			default:
				Log.d(LOGTAG, "not supported btn id");
			}
		}
	};

	/**
	 * 更新服务器地址
	 */
	private void startRefreshList() {
		// 发送开始刷新的消息
		onRefreshStart();

		Log.d(LOGTAG, "===> start refresh playlist");

		// 这里创建一个脱离UI主线程的线程负责网络下载
		new Thread() {
			public void run() {
				// 到远程服务器下载直播电视播放列表
				tvPlaylistDownload();

				isTVListSuc = sharedPreferences
						.getBoolean("isTVListSuc", false);
				
				// 发送刷新完毕的消息
				onRefreshEnd();
				Log.d(LOGTAG, "===> end refresh playlist");
			};
		}.start();

	}

	/**
	 * 更新界面的节目表list
	 */
	private void RefreshList() {
		// 置已经备份过的标志位
		editor.putBoolean("hasBackup", true);
		editor.commit();
		// 重新加载当前的播放列表
		if (allinfos != null) {

			// 首先清空之前的数据
			allinfos.clear();
			yangshi_infos.clear();
			weishi_infos.clear();
			difang_infos.clear();
			tiyu_infos.clear();
			yule_infos.clear();
			qita_infos.clear();

			// 重新解析XML
			allinfos = ParseUtil.parse(ChannelTabActivity.this, true);
			// 重新显示list
			setYangshiView();
			setWeishiView();
			setDifangView();
			setTiyuView();
			setYuleView();
			setQitaView();
		}
	}

	/**
	 * FTP下载单个文件测试
	 */
	private void tvPlaylistDownload() {
		FTPClient ftpClient = new FTPClient();
		FileOutputStream fos = null;

		// 3秒钟，如果超过就判定超时了
		ftpClient.setConnectTimeout(3000);

		// 假设更新列表成功
		editor.putBoolean("isTVListSuc", true);
		editor.commit();

		// TODO 后续可以设置多个服务器地址，防止服务器流量不够，导致更新失败
		try {
			int reply;

			// 设置编码格式
			ftpClient.setControlEncoding("UTF-8");
			// 连接服务器
			ftpClient.connect("ftp92147.host217.web519.com");

			reply = ftpClient.getReplyCode();
			if (!FTPReply.isPositiveCompletion(reply)) {
				// 断开连接
				ftpClient.disconnect();
				// 更新列表失败
				editor.putBoolean("isTVListSuc", false);
				editor.commit();
				return;
			}

			// 用户登录信息
			ftpClient.login("ftp92147", "950288@kk");

			// 此处不需要Data前面的"/"
			String remoteFileName = "Data/channel_list_cn.list.api2";
			// 此处要注意必须加上channel_list_cn.list.api2前面的"/"
			fos = new FileOutputStream(Environment
					.getExternalStorageDirectory().getPath()
					+ "/kekePlayer/.channel_list_cn.list.api2");

			ftpClient.setBufferSize(1024);
			// 设置文件类型（二进制）
			ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE);
			// 使用FTP被动模式（让FTP服务器每次都开同一个端口发送数据）
			ftpClient.enterLocalPassiveMode();
			// 下载文件
			ftpClient.retrieveFile(remoteFileName, fos);
		} catch (IOException e) {
			e.printStackTrace();
			// 更新列表失败
			editor.putBoolean("isTVListSuc", false);
			editor.commit();
			// throw new RuntimeException("FTP客户端出错！", e);
		} finally {
			try {
				if (fos != null) {
					// TODO 需要对文件的合法性作一定的测试，例如大小
					fos.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
				// 更新列表失败
				editor.putBoolean("isTVListSuc", false);
				editor.commit();
				// throw new RuntimeException("关闭文件发生异常！", e);
			}
			try {
				// 用户注销
				ftpClient.logout();
				// FTP断开连接
				ftpClient.disconnect();
			} catch (IOException e) {
				e.printStackTrace();
				// 更新列表失败
				editor.putBoolean("isTVListSuc", false);
				editor.commit();
				// throw new RuntimeException("关闭FTP连接发生异常！", e);
			}
		}
	}

	private static Handler mEventHandler;
	private static final int TV_LIST_REFRESH_START = 0x0001;
	private static final int TV_LIST_REFRESH_END = 0x0002;

	/**
	 * 地址刷新过程中的事件响应的核心处理方法
	 */
	private void initializeEvents() {
		mEventHandler = new Handler() {
			public void handleMessage(Message msg) {
				switch (msg.what) {
				case TV_LIST_REFRESH_START:
					// 开始刷新，开始转圈
					if (operatingAnim != null) {
						button_refresh.startAnimation(operatingAnim);
					}
					break;
				case TV_LIST_REFRESH_END:
					// 刷新完毕，停止转圈
					button_refresh.clearAnimation();
					// 处理刷新结果
					dealRefreshResult();

					break;
				default:
					break;
				}
			}
		};
	}

	/**
	 * 处理刷新的结果，判断刷新成功还是失败
	 * 如果成功，就更新位服务器上的直播地址
	 * 如果失败，仍采用当前的直播地址
	 */
	private void dealRefreshResult() {
		if (isTVListSuc) {
			// 更新界面的节目表list
			RefreshList();
			// 显示对话框
			// 弹出加载【成功】对话框
			if (ChannelTabActivity.this == null)
				return;
			new AlertDialog.Builder(ChannelTabActivity.this)
					.setTitle("更新成功")
					.setMessage("服务器地址更新成功")
					.setNegativeButton("知道了",
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									// do nothing - it will close on its
									// own
								}
							}).show();
		} else {
			// 弹出加载【失败】对话框
			if (ChannelTabActivity.this == null)
				return;
			new AlertDialog.Builder(ChannelTabActivity.this)
					.setTitle("更新失败")
					.setMessage("抱歉！服务器地址更新失败\n已为您加载备份的节目地址")
					.setNegativeButton("知道了",
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									// do nothing - it will close on its
									// own
								}
							}).show();
		}
	}

	/**
	 * 以下：接收事件，做中间处理，再调用handleMessage方法处理之
	 * 
	 * @{
	 */
	private void onRefreshStart() {
		Message msg = new Message();
		msg.what = TV_LIST_REFRESH_START;
		mEventHandler.sendMessage(msg);
	}

	private void onRefreshEnd() {
		Message msg = new Message();
		msg.what = TV_LIST_REFRESH_END;
		mEventHandler.sendMessage(msg);
	}

}

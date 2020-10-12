/**
 * Copyright (c) 2019,2020 honintech
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the “Software”), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 * 
 */
package cn.weforward.common.log;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownServiceException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.Layout;
import cn.weforward.common.crypto.Base64;
import cn.weforward.common.json.JsonOutputStream;
import cn.weforward.common.json.JsonUtil;
import cn.weforward.common.util.RingBuffer;
import cn.weforward.common.util.StringBuilderPool;
import cn.weforward.common.util.StringUtil;

/**
 * 延迟远程写日志追加类
 * 
 * @author daibo
 *
 */
public class DelayRemoteAppender extends AppenderBase<ILoggingEvent> implements Runnable {
	/** 默认主题 */
	static final String DEFAULT_SUBJECT_PATTERN = "%logger{20}";
	/** 行分隔符 */
	static final String DEFAULT_SERVER = "unkonwn";
	/** 对象锁 */
	final Object m_Lock = new Object();
	/** 服务器标识 */
	protected String m_Server = DEFAULT_SERVER;
	/** 主题 */
	protected String m_Subject = DEFAULT_SUBJECT_PATTERN;
	/** 主题布局 */
	protected Layout<ILoggingEvent> m_SubjectLayout;
	/** 内容布局 */
	protected Layout<ILoggingEvent> m_ContentLayout;
	/** 缓存的vo */
	protected RingBuffer<SenderVo> m_Items;
	/** 处理间隔 */
	protected long m_Interval = 1000l;
	/** 最大量 */
	protected long m_MaxSupport = 100;
	/** 远程url */
	protected List<URL> m_Urls;
	/** 方法名 */
	protected String m_Method = "write";
	/** 线程 */
	protected Thread m_Thread;
	/** 项目名 */
	protected String m_ProjectName;
	/** 服务id */
	protected String m_Serverid;
	/** 访问帐号 */
	protected String m_UserName;
	/** 访问密码 */
	protected String m_Password;
	/** 连接超时值（毫秒） */
	protected int m_ConnectTimeout = 3 * 1000;
	/** 读/等待结果超时值（毫秒） */
	protected int m_ReadTimeout = 60 * 1000;
	
	protected String m_Authorization;

	// /** StringBuilder池 */
	// protected static StringBuilderPool _Pool = new StringBuilderPool(128,
	// 4096);

	@Override
	public void start() {
		m_Items = new RingBuffer<SenderVo>(1024);
		Thread thread = new Thread(this, "DelayRemoteAppender");
		thread.setDaemon(true);
		m_Thread = thread;
		m_SubjectLayout = makeSubjectLayout(m_Subject);
		thread.start();
		super.start();
	}

	@Override
	public void stop() {
		super.stop();
		synchronized (m_Lock) {
			m_Thread = null;
			m_Lock.notify();
		}
	}

	public void setUrl(String url) throws MalformedURLException {
		setUrls(Collections.singletonList(url));
	}

	public void setUrls(List<String> urls) throws MalformedURLException {
		if (null == urls || urls.size() == 0) {
			m_Urls = Collections.emptyList();
			return;
		}
		List<URL> ls = new ArrayList<>(urls.size());
		for (String url : urls) {
			ls.add(new URL(url));
		}
		m_Urls = ls;
	}

	public void setMethod(String method) {
		m_Method = method;
	}

	public void setProjectName(String projectName) {
		m_ProjectName = projectName;
	}

	public void setServerid(String sid) {
		m_Serverid = sid;
	}

	public void setServer(String server) {
		m_Server = server;
	}

	public void setSubject(String subject) {
		m_Subject = subject;
	}

	public void setInterval(int interval) {
		m_Interval = interval;
	}

	public void setMaxSupport(int max) {
		m_MaxSupport = max;
	}

	/** 访问帐号 */
	public void setUserName(String name) {
		m_UserName = name;
		m_Authorization = null;
	}

	/** 访问密码 */
	public void setPassword(String password) {
		m_Password = password;
		m_Authorization = null;
	}

	/** 连接超时值（毫秒） */
	public void setConnectTimeout(int t) {
		m_ConnectTimeout = t;
	}

	/** 读/等待结果超时值（毫秒） */
	public void setReadTimeout(int t) {
		m_ReadTimeout = t;
	}

	protected Layout<ILoggingEvent> makeSubjectLayout(String subjectStr) {
		PatternLayout pl = new PatternLayout();
		pl.setContext(getContext());
		pl.setPattern(subjectStr);
		pl.setPostCompileProcessor(null);
		pl.start();
		return pl;
	}

	public Layout<ILoggingEvent> getLayout() {
		return m_ContentLayout;
	}

	public void setLayout(Layout<ILoggingEvent> layout) {
		this.m_ContentLayout = layout;
	}

	@Override
	protected void append(ILoggingEvent eventObject) {
		if (null == m_Thread || null == m_Items) {
			return;// 线程没开始不允许加元素
		}
		SenderVo vo = new SenderVo(getSubject(eventObject), getContent(eventObject), getLevel(eventObject));
		m_Items.offer(vo);
	}

	private String getSubject(ILoggingEvent event) {
		String subjectStr = "Undefined subject";
		if (m_SubjectLayout != null) {
			subjectStr = m_SubjectLayout.doLayout(event);
		}
		return subjectStr;
	}

	private String getContent(ILoggingEvent event) {
		Layout<ILoggingEvent> layout = m_ContentLayout;
		// StringBuilder content = new StringBuilder();
		StringBuilder content = StringBuilderPool._8k.poll();
		try {
			String header = layout.getFileHeader();
			if (header != null) {
				content.append(header);
			}
			String presentationHeader = layout.getPresentationHeader();
			if (presentationHeader != null) {
				content.append(presentationHeader);
			}
			content.append(layout.doLayout(event));
			String presentationFooter = layout.getPresentationFooter();
			if (presentationFooter != null) {
				content.append(presentationFooter);
			}
			String footer = layout.getFileFooter();
			if (footer != null) {
				content.append(footer);
			}
			return content.toString();
		} finally {
			StringBuilderPool._8k.offer(content);
		}
	}

	private String getLevel(ILoggingEvent event) {
		Level l = event.getLevel();
		return null == l ? "" : l.levelStr;
	}

	@Override
	public void run() {
		while (null != m_Thread) {
			try {
				do {
					send();
					synchronized (m_Lock) {
						try {
							m_Lock.wait(m_Interval);
						} catch (InterruptedException e) {
							Thread.currentThread().interrupt();
							// 结束运行
							return;
						}
						if (null == m_Thread) {
							// 结束运行
							return;
						}
					}
				} while (null != m_Thread);
			} catch (Throwable e) {
				try {
					addError("运行出错", e);
				} catch (Throwable error) {
					// 不处理
				}
			}
			synchronized (this) {
				try {
					wait(10 * 1000);
				} catch (InterruptedException e) {
					return;
				}
			}
		}
	}

	private void send() {
		try {
			SenderVo vo;
			while (null != (vo = m_Items.remove())) {
				send(vo.subject, vo.content, vo.level);
			}
		} catch (Exception e) {
			addError("Error occurred while sending Remote.", e);
		}
	}

	private String getServer() {
		if (StringUtil.eq(m_Server, DEFAULT_SERVER)) {
			if (!StringUtil.isEmpty(m_ProjectName)) {
				return m_ProjectName + "_" + m_Serverid;
			}
		}
		return m_Server;
	}

	private void send(String subject, String content, String level) {
		for (URL url : m_Urls) {
			try {
				send(url, getServer(), subject, content, level);
			} catch (Exception e) {
				addError("Error occurred while remote", e);
			}
		}

	}

	private void send(URL url, String server, String subject, String content, String level) throws IOException {
		HttpURLConnection http = null;
		// try {
		// URL url = new URL(serviceUrl);
		http = (HttpURLConnection) url.openConnection();
		http.setConnectTimeout(m_ConnectTimeout);
		http.setReadTimeout(m_ReadTimeout);
		http.setChunkedStreamingMode(0);
		http.setDoInput(true);
		http.setDoOutput(true);
		http.setUseCaches(false);
		http.setRequestMethod("POST");
		if (null == m_Authorization) {
			// 生成Basic验证
			if (!StringUtil.isEmpty(m_UserName) && !StringUtil.isEmpty(m_Password)) {
				m_Authorization = "Basic " + Base64.encode((m_UserName + ":" + m_Password).getBytes());
			} else {
				m_Authorization = "";
			}
		}
		if (!StringUtil.isEmpty(m_Authorization)) {
			http.setRequestProperty("Authorization", m_Authorization);
		}
		OutputStream out = http.getOutputStream();
		JsonOutputStream builder = new JsonOutputStream(out);
		builder.append("{");
		builder.append("\"server\":\"");
		JsonUtil.escape(server, builder);
		builder.append("\",");
		builder.append("\"subject\":\"");
		JsonUtil.escape(subject, builder);
		builder.append("\",");
		builder.append("\"content\":\"");
		JsonUtil.escape(content, builder);
		builder.append("\",");
		builder.append("\"level\":\"");
		JsonUtil.escape(level, builder);
		builder.append("\"}");
		out.flush();
		builder.close();
		int status = http.getResponseCode();
		http.getInputStream().close();
		if (HttpURLConnection.HTTP_OK != status) {
			throw new UnknownServiceException(url + " 日志服务异常：" + status + " " + http.getResponseMessage());
		}
		// } catch (IOException e) {
		// throw e;
		// }
	}

	/**
	 * 发送的vo
	 * 
	 * @author daibo
	 * 
	 */
	static class SenderVo implements Comparable<SenderVo> {
		/** 主题 */
		String subject;
		/** 内容 */
		String content;
		/** 等级 */
		String level;
		/** 数量 */
		int num;

		public SenderVo(String subject, String content, String level) {
			this.subject = subject;
			this.content = content;
			this.level = level;
			this.num = 0;
		}

		public String getSubject() {
			return subject;
		}

		public String getLevel() {
			return level;
		}

		@Override
		public int compareTo(SenderVo o) {
			int i = o.subject.compareTo(subject);
			if (i == 0) {
				return o.level.compareTo(level);
			}
			return i;
		}
	}
}

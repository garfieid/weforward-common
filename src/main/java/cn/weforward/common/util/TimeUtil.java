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
package cn.weforward.common.util;

import java.io.IOException;
import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.LoggerFactory;

import cn.weforward.common.Nameable;

/**
 * 日期/时间相关方法
 * 
 * @version V1.0
 * 
 * @author liangyi
 */
public class TimeUtil {
	/** 日志记录器 */
	public final static org.slf4j.Logger _Logger = LoggerFactory.getLogger(TimeUtil.class);

	/** 当前时区（毫秒） */
	public final static int TIMEZONE_OFFSET = TimeZone.getDefault().getRawOffset();

	/** 一小时的毫秒数 */
	public final static int HOUR_MILLS = 3600 * 1000;

	/** 一天的毫秒数 */
	public final static int DAY_MILLS = 24 * HOUR_MILLS;

	/** GMT时区下的1970-1-1零时的日期时间 */
	public final static Date GMT1970 = new ReadonlyDate(0);

	/** 大概1000年后的将来 */
	public static final Date FAR_FUTURE = new ReadonlyDate(
			System.currentTimeMillis() + 1000 * 365L * DAY_MILLS);

	/** 日期/时间格式器共享实例表 */
	static final ConcurrentMap<String, DateFormatPool> _DateFormats = new ConcurrentHashMap<>(16);

	/** yyyyMMddHHmmss紧凑日期时间格式 */
	final static DateFormat DTF_COMPACT = getDateFormatInstance("yyyyMMddHHmmss");// new
																					// SimpleDateFormat("yyyyMMddHHmmss");
	/** yyyy-MM-dd HH:mm:ss 一般日期时间格式 */
	final static DateFormat DTF_GENERAL = getDateFormatInstance("yyyy-MM-dd HH:mm:ss");// new
																						// SimpleDateFormat("yyyy-MM-dd
																						// HH:mm:ss");
	/** yyyy-M-d H:m:s 一般日期时间格式 */
	final static DateFormat DTF_GENERAL_PARSE = getDateFormatInstance("yyyy-M-d H:m:s");// new
	// SimpleDateFormat("yyyy-M-d H:m:s");
	/** yyyy-MM-ddTHH:mm:ss 有“T”分隔的一般日期时间格式 */
	final static DateFormat DTF_GENERAL_T = getDateFormatInstance("yyyy-MM-dd'T'HH:mm:ss");// new
																							// SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
	/** yyyy-MM-dd 一般日期格式 */
	final static DateFormat DF_GENERAL = getDateFormatInstance("yyyy-MM-dd");// new
																				// SimpleDateFormat("yyyy-MM-dd");
	/** yyyy-MM-dd 一般时间格式 */
	final static DateFormat TF_GENERAL = getDateFormatInstance("HH:mm:ss");// new
																			// SimpleDateFormat("HH:mm:ss");
	/** GMT 时间格式，如：Tue, 18 Aug 2009 14:11:02 GMT */
	final static DateFormat DTF_GMT = new DateFormatPool("EEE, d MMM yyyy HH:mm:ss z",
			Locale.ENGLISH);
	/** CST 时间格式，如：Tue Aug 18 22:11:02 CST 2009 */
	final static DateFormat DTF_CST = new DateFormatPool("EEE MMM d HH:mm:ss z yyyy",
			Locale.ENGLISH);
	/** UTC 时间格式，如：2009-8-18T22:31:02.000+0800 */
	final static DateFormat DTF_UTC = getDateFormatInstance("yyyy-MM-dd'T'HH:mm:ss.SSSZ");// new
	// SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
	/** Weforward的GMT格式 */
	static final DateFormat WF_DATE_TIME_FORMAT = getDateFormatInstance(
			"yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

	/** GMT时区 */
	public final static TimeZone TZ_GMT = TimeZone.getTimeZone("GMT");

	/** GMT日历对象池 */
	static final RingBuffer<Calendar> _GmtCalendarPool = new RingBuffer<Calendar>(64) {
		@Override
		protected Calendar onEmpty() {
			return Calendar.getInstance(TimeUtil.TZ_GMT);
		}
	};

	/**
	 * 初始化DTF_GMT的时区为0
	 */
	static {
		DTF_GMT.setTimeZone(TZ_GMT);
		WF_DATE_TIME_FORMAT.setTimeZone(TZ_GMT);
	}

	/**
	 * 转换字串格式的日期时间为Date对象（支持多种）
	 * 
	 * @param str
	 *            日期时间格式串，如：20090818T223102000,20090818,yyyy-MM-dd
	 *            HH:mm:ss,2009-
	 *            8-18T22:31:02.000+0800,yyyy-MM-dd'T'HH:mm:ss,yyyy-MM-dd等
	 * @return 转换后的Date对象，若格式无效返回null
	 */
	public static Date parseDate(String str) {
		if (null == str || str.length() < 4) {
			return null;
		}
		Date d = null;
		Calendar rc = null;
		try {
			if (18 == str.length() && 'T' == str.charAt(8)) {
				// 检查格式是否为yyyyMMddTHHmmssSSS（如：20090818T223102000）
				try {
					rc = _GmtCalendarPool.poll();
					rc.clear();
					rc.set(Calendar.YEAR, Integer.parseInt(str.substring(0, 4)));
					rc.set(Calendar.MONTH, Integer.parseInt(str.substring(4, 6)) - 1);
					rc.set(Calendar.DAY_OF_MONTH, Integer.parseInt(str.substring(6, 8)));
					rc.set(Calendar.HOUR_OF_DAY, Integer.parseInt(str.substring(9, 11)));
					rc.set(Calendar.MINUTE, Integer.parseInt(str.substring(11, 13)));
					rc.set(Calendar.SECOND, Integer.parseInt(str.substring(13, 15)));
					rc.set(Calendar.MILLISECOND, Integer.parseInt(str.substring(15, 18)));
					return rc.getTime();
					// }
				} catch (NumberFormatException e) {
					_Logger.warn("解析" + str + "异常", e);
				}
			} else if (8 == str.length() && NumberUtil.isNumber(str)) {
				// 检查格式是否为yyyyMMdd（如：20090818）
				try {
					rc = _GmtCalendarPool.poll();
					rc.clear();
					rc.set(Calendar.YEAR, Integer.parseInt(str.substring(0, 4)));
					rc.set(Calendar.MONTH, Integer.parseInt(str.substring(4, 6)) - 1);
					rc.set(Calendar.DAY_OF_MONTH, Integer.parseInt(str.substring(6, 8)));
					return rc.getTime();
					// }
				} catch (NumberFormatException e) {
					_Logger.warn("解析" + str + "异常", e);
				}
			}
		} finally {
			_GmtCalendarPool.offer(rc);
		}
		try {
			if (24 == str.length() && 'T' == str.charAt(10)) {
				synchronized (WF_DATE_TIME_FORMAT) {
					return WF_DATE_TIME_FORMAT.parse(str);
				}
			}
			if (-1 != str.indexOf('-')) {
				// 估计是DTF_GENERAL或DF_GENERAL或DTF_GENERAL_T
				if (-1 != str.indexOf(' ')) {
					// DTF_GENERAL_PARSE 格式
					d = DTF_GENERAL_PARSE.parse(str);
				} else if (-1 != str.indexOf('.')) {
					// UTC 时间
					d = DTF_UTC.parse(str);
				} else if (-1 != str.indexOf('T')) {
					// DTF_GENERAL_T 格式
					d = DTF_GENERAL_T.parse(str);
				} else {
					// DF_GENERAL 格式
					d = DF_GENERAL.parse(str);
				}
			} else {
				String last = str.substring(str.length() - 4).toUpperCase();
				if (" GMT".equals(last) || " UTC".equals(last) || " CST".equals(last)) {
					// GMT 时间
					d = DTF_GMT.parse(str);
				} else if (-1 != str.indexOf(" CST ")) {
					// GMT 时间
					d = DTF_CST.parse(str);
				} else if (-1 != str.indexOf('.')) {
					// UTC 时间
					d = DTF_UTC.parse(str);
				} else if (-1 != str.indexOf(':')) {
					// 可能只有时间部分
					d = TF_GENERAL.parse(str);
				}
			}
		} catch (ParseException e) {
			_Logger.warn("解析" + str + "异常", e);
		}
		return d;
	}

	/**
	 * 转为日期时间格式字串（yyyy-MM-dd HH:mm:ss）
	 * 
	 * @param date
	 *            日期时间
	 * @return 格式串，若date为null则返回长度为0的字串
	 */
	static public final String formatDateTime(Date date) {
		if (null == date) {
			return "";
		}
		// synchronized (DTF_GENERAL) {
		return DTF_GENERAL.format(date);
		// }
	}

	/**
	 * 转为日期格式字串
	 * 
	 * @param date
	 *            日期
	 * @return yyyy-MM-dd 日期格式串
	 */
	static public final String formatDate(Date date) {
		// synchronized (DF_GENERAL) {
		return DF_GENERAL.format(date);
		// }
	}

	/**
	 * 转为时间格式字串
	 * 
	 * @param date
	 *            时间
	 * @return HH:mm:ss 时间格式串
	 */
	static public final String formatTime(Date date) {
		// synchronized (TF_GENERAL) {
		return TF_GENERAL.format(date);
		// }
	}

	/**
	 * 转为UTC日期时间格式字串
	 * 
	 * @param date
	 *            日期时间
	 * @return yyyy-MM-dd'T'HH:mm:ss.SSSZ 包含时区信息的日期时间格式串
	 */
	static public final String formatUTC(Date date) {
		// synchronized (DTF_UTC) {
		return DTF_UTC.format(date);
		// }
	}

	/**
	 * 格式化为GMT日期时间串
	 * 
	 * @param date
	 *            日期时间
	 * @return 格式串，如：Tue, 18 Aug 2009 14:11:02 GMT
	 */
	static public final String formatGMT(Date date) {
		// synchronized (DTF_GMT) {
		return DTF_GMT.format(date);
		// }
	}

	/**
	 * 格式化为紧凑GMT日期时间格式字串
	 * 
	 * @param date
	 *            日期时间
	 * @return 格式串，如：20090818T223102000
	 */
	static public final String formatCompactGMT(Date date) {
		return formatCompactGMT(date, new StringBuilder(18)).toString();
	}

	/**
	 * 格式化为紧凑GMT日期时间格式字串，如：20090818T223102000
	 * 
	 * @param date
	 *            日期时间
	 * @param sb
	 *            把格式串追加至其
	 * @return 传入的sb
	 */
	static public final StringBuilder formatCompactGMT(Date date, StringBuilder sb) {
		Calendar rc = _GmtCalendarPool.poll();
		try {
			rc.clear();
			rc.setTime(date);
			append4(sb, rc.get(Calendar.YEAR));
			append2(sb, 1 + rc.get(Calendar.MONTH));
			append2(sb, rc.get(Calendar.DAY_OF_MONTH));
			sb.append('T');
			append2(sb, rc.get(Calendar.HOUR_OF_DAY));
			append2(sb, rc.get(Calendar.MINUTE));
			append2(sb, rc.get(Calendar.SECOND));
			append3(sb, rc.get(Calendar.MILLISECOND));
		} finally {
			_GmtCalendarPool.offer(rc);
		}
		return sb;
	}

	/**
	 * 转为紧凑GMT日期格式字串，如：20090818
	 * 
	 * @param date
	 *            日期
	 * @return 格式串
	 */
	static public final String formatCompactDateGMT(Date date) {
		return formatCompactDateGMT(date, new StringBuilder(8)).toString();
	}

	/**
	 * 转为紧凑GMT日期格式字串，如：20090818
	 * 
	 * @param date
	 *            日期
	 * @param sb
	 *            把格式串追加至其
	 * @return 传入的sb
	 */
	static public final StringBuilder formatCompactDateGMT(Date date, StringBuilder sb) {
		Calendar rc = _GmtCalendarPool.poll();
		try {
			rc.clear();
			rc.setTime(date);
			append4(sb, rc.get(Calendar.YEAR));
			append2(sb, 1 + rc.get(Calendar.MONTH));
			append2(sb, rc.get(Calendar.DAY_OF_MONTH));
		} finally {
			_GmtCalendarPool.offer(rc);
		}
		return sb;
	}

	/**
	 * 格式化GMT时区的时间为紧凑的“年月日时”格式串，如“2009081822”，其通常用于按小时为段建立查询索引或统计项
	 * 
	 * @param date
	 *            日期时间
	 * @param sb
	 *            把格式串追加至其
	 * @return 传入的sb
	 */
	static public final StringBuilder formatYyyyMmDdHhGMT(Date date, StringBuilder sb) {
		Calendar rc = _GmtCalendarPool.poll();
		try {
			rc.clear();
			rc.setTime(date);
			append4(sb, rc.get(Calendar.YEAR));
			append2(sb, 1 + rc.get(Calendar.MONTH));
			append2(sb, rc.get(Calendar.DAY_OF_MONTH));
			append2(sb, rc.get(Calendar.HOUR_OF_DAY));
		} finally {
			_GmtCalendarPool.offer(rc);
		}
		return sb;
	}

	/**
	 * 格式化GMT时区的时间为紧凑的“年月日时”格式串，如“2009081822”，其通常用于按小时为段建立查询索引或统计项
	 * 
	 * @param millis
	 *            （毫秒的）日期时间
	 * @param sb
	 *            把格式串追加至其
	 * @return 传入的sb
	 */
	static public final StringBuilder formatYyyyMmDdHhGMT(long millis, StringBuilder sb) {
		Calendar rc = _GmtCalendarPool.poll();
		try {
			rc.setTimeInMillis(millis);
			append4(sb, rc.get(Calendar.YEAR));
			append2(sb, 1 + rc.get(Calendar.MONTH));
			append2(sb, rc.get(Calendar.DAY_OF_MONTH));
			append2(sb, rc.get(Calendar.HOUR_OF_DAY));
		} finally {
			_GmtCalendarPool.offer(rc);
		}
		return sb;
	}

	/**
	 * 格式化GMT时区的时间为紧凑的“年月日时”格式串，如“2009081822”，其通常用于按小时为段建立查询索引或统计项
	 * 
	 * @param date
	 *            日期时间
	 * @param sb
	 *            把格式串追加至其
	 * @return 传入的sb
	 */
	static public final String formatYyyyMmDdHhGMT(Date date) {
		return formatYyyyMmDdHhGMT(date, new StringBuilder(10)).toString();
	}

	/**
	 * 转为紧凑（当前时区）日期格式字串，如：20090818
	 * 
	 * @param date
	 *            日期
	 * @return 日期格式串
	 */
	static public final String formatCompactDate(Date date) {
		return formatCompactDate(date, new StringBuilder(8)).toString();
	}

	/**
	 * 转为紧凑（当前时区）日期格式字串，如：20090818
	 * 
	 * @param date
	 *            日期
	 * @param sb
	 *            StringBuilder
	 * @return 日期格式串
	 */
	static public final StringBuilder formatCompactDate(Date date, StringBuilder sb) {
		Calendar c = Calendar.getInstance();
		c.setTime(date);
		append4(sb, c.get(Calendar.YEAR));
		append2(sb, 1 + c.get(Calendar.MONTH));
		append2(sb, c.get(Calendar.DAY_OF_MONTH));
		return sb;
	}

	/**
	 * 转为紧凑（当前时区）日期时间格式字串yyyyMMddHHmmss，如：20090818010203
	 * 
	 * @param date
	 *            日期
	 * @param sb
	 *            StringBuilder
	 * @return 日期格式串
	 */
	static public final StringBuilder formatCompactDateTime(Date date, StringBuilder sb) {
		Calendar c = Calendar.getInstance();
		c.setTime(date);
		append4(sb, c.get(Calendar.YEAR));
		append2(sb, 1 + c.get(Calendar.MONTH));
		append2(sb, c.get(Calendar.DAY_OF_MONTH));
		append2(sb, c.get(Calendar.HOUR_OF_DAY));
		append2(sb, c.get(Calendar.MINUTE));
		append2(sb, c.get(Calendar.SECOND));
		return sb;
	}

	/**
	 * 转为紧凑（当前时区）日期时间格式字串yyyyMMddHHmmss，如：20090818010203
	 * 
	 * @param date
	 *            日期 StringBuilder
	 * @return 日期格式串
	 */
	static public final String formatCompactDateTime(Date date) {
		return formatCompactDateTime(date, new StringBuilder(14)).toString();
	}

	/**
	 * 格式化为紧凑的GMT时区的时间戳，如：20140311T181509000
	 * 
	 * @param timestamp
	 *            时间戳（Date.getTime()）
	 * @param sb
	 *            把格式串追加至其，若为null则内部创建
	 * @return 格式化输出
	 */
	static public final StringBuilder formatTimestamp(long timestamp, StringBuilder sb) {
		if (null == sb) {
			sb = new StringBuilder(18);
		}
		Calendar rc = _GmtCalendarPool.poll();
		try {
			rc.clear();
			;
			rc.setTimeInMillis(timestamp);
			append4(sb, rc.get(Calendar.YEAR));
			append2(sb, 1 + rc.get(Calendar.MONTH));
			append2(sb, rc.get(Calendar.DAY_OF_MONTH));
			sb.append('T');
			append2(sb, rc.get(Calendar.HOUR_OF_DAY));
			append2(sb, rc.get(Calendar.MINUTE));
			append2(sb, rc.get(Calendar.SECOND));
			append3(sb, rc.get(Calendar.MILLISECOND));
		} finally {
			_GmtCalendarPool.offer(rc);
		}
		return sb;
	}

	/**
	 * 格式化为紧凑的GMT时区的时间戳，如：20140311T181509000
	 * 
	 * @param timestamp
	 *            时间戳（Date.getTime()）
	 * @param appender
	 *            把格式串追加至其，若为null则内部创建
	 * @return 格式化输出
	 */
	static public final void formatTimestamp(long timestamp, Appendable appender) {
		if (null == appender) {
			appender = new StringBuilder(18);
		}
		Calendar rc = _GmtCalendarPool.poll();
		try {
			rc.clear();
			;
			rc.setTimeInMillis(timestamp);
			append4(appender, rc.get(Calendar.YEAR));
			append2(appender, 1 + rc.get(Calendar.MONTH));
			append2(appender, rc.get(Calendar.DAY_OF_MONTH));
			appender.append('T');
			append2(appender, rc.get(Calendar.HOUR_OF_DAY));
			append2(appender, rc.get(Calendar.MINUTE));
			append2(appender, rc.get(Calendar.SECOND));
			append3(appender, rc.get(Calendar.MILLISECOND));
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			_GmtCalendarPool.offer(rc);
		}
	}

	/**
	 * 取得当前（GMT）时间的格式串yyyyMMddHHmm，如：201708140102
	 */
	static public final String getTimestampYmdhm() {
		return formatTimestampYmdhm(currentTimeMillis(), null).toString();
	}

	/**
	 * 格式化（GMT）时间的格式串yyyyMMddHHmm，如：201708140102
	 * 
	 * @param timestamp
	 *            时间戳（Date.getTime()）
	 * @param sb
	 *            把格式串追加至其，若为null则内部创建
	 * @return 格式化输出
	 */
	static public final StringBuilder formatTimestampYmdhm(long ms, StringBuilder sb) {
		if (null == sb) {
			sb = new StringBuilder(12);
		}
		Calendar rc = _GmtCalendarPool.poll();
		try {
			rc.clear();
			;
			rc.setTimeInMillis(ms);
			append4(sb, rc.get(Calendar.YEAR));
			append2(sb, 1 + rc.get(Calendar.MONTH));
			append2(sb, rc.get(Calendar.DAY_OF_MONTH));
			append2(sb, rc.get(Calendar.HOUR_OF_DAY));
			append2(sb, rc.get(Calendar.MINUTE));
		} finally {
			_GmtCalendarPool.offer(rc);
		}
		return sb;
	}

	/**
	 * 格式化（GMT）时间的格式串yyMMddHHmm，如：1708140102
	 * 
	 * @param timestamp
	 *            时间戳（Date.getTime()）
	 * @param sb
	 *            把格式串追加至其，若为null则内部创建
	 * @return 格式化输出
	 */
	static public final StringBuilder formatTimestampMdhm(long ms, StringBuilder sb) {
		if (null == sb) {
			sb = new StringBuilder(10);
		}
		Calendar rc = _GmtCalendarPool.poll();
		try {
			rc.clear();
			;
			rc.setTimeInMillis(ms);
			append2(sb, rc.get(Calendar.YEAR) % 1000);
			append2(sb, 1 + rc.get(Calendar.MONTH));
			append2(sb, rc.get(Calendar.DAY_OF_MONTH));
			append2(sb, rc.get(Calendar.HOUR_OF_DAY));
			append2(sb, rc.get(Calendar.MINUTE));
		} finally {
			_GmtCalendarPool.offer(rc);
		}
		return sb;
	}

	/**
	 * 转换（GMT）时间戳格式串的日期时间为Date对象（支持多种）
	 * 
	 * @param str
	 *            （GMT）时间戳格式串，如：20140311T181509000，
	 *            2009，200908，20090818，2009081801，200908180102，20090818010203
	 * @return 转换后的Date对象，若格式无效返回null
	 */
	public static Date parseTimestamp(String str) {
		if (null == str || str.length() < 4) {
			return null;
		}
		Calendar rc = null;
		try {
			if (18 == str.length() && 'T' == str.charAt(8)) {
				// 检查格式是否为yyyyMMddTHHmmssSSS（如：20090818T223102000）
				rc = _GmtCalendarPool.poll();
				rc.clear();
				;
				rc.set(Calendar.YEAR, Integer.parseInt(str.substring(0, 4)));
				rc.set(Calendar.MONTH, Integer.parseInt(str.substring(4, 6)) - 1);
				rc.set(Calendar.DAY_OF_MONTH, Integer.parseInt(str.substring(6, 8)));
				rc.set(Calendar.HOUR_OF_DAY, Integer.parseInt(str.substring(9, 11)));
				rc.set(Calendar.MINUTE, Integer.parseInt(str.substring(11, 13)));
				rc.set(Calendar.SECOND, Integer.parseInt(str.substring(13, 15)));
				rc.set(Calendar.MILLISECOND, Integer.parseInt(str.substring(15, 18)));
				return rc.getTime();
			}
			if (!NumberUtil.isNumber(str)) {
				// 有非数字（肯定不对路）
				return null;
			}
			rc = _GmtCalendarPool.poll();
			rc.clear();
			// 年
			rc.set(Calendar.YEAR, Integer.parseInt(str.substring(0, 4)));
			if (str.length() >= 6) {
				// 月
				rc.set(Calendar.MONTH, Integer.parseInt(str.substring(4, 6)) - 1);
				if (str.length() >= 8) {
					// 日
					rc.set(Calendar.DAY_OF_MONTH, Integer.parseInt(str.substring(6, 8)));
					if (str.length() >= 10) {
						// 时
						rc.set(Calendar.HOUR_OF_DAY, Integer.parseInt(str.substring(8, 10)));
						if (str.length() >= 12) {
							// 分
							rc.set(Calendar.MINUTE, Integer.parseInt(str.substring(10, 12)));
							if (str.length() >= 14) {
								// 秒
								rc.set(Calendar.SECOND, Integer.parseInt(str.substring(12, 14)));
							}
						}
					}
				}
			}
			return rc.getTime();
		} catch (NumberFormatException e) {
			_Logger.warn("解析" + str + "异常", e);
		} finally {
			_GmtCalendarPool.offer(rc);
		}
		return null;
	}

	/** 十进制数字（0~9） */
	protected final static char[] _TenDigits = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' };

	// static final int _TenStep[] = { 1000000000, 100000000, 10000000, 1000000,
	// 100000, 10000, 1000,
	// 100, 10 };

	/**
	 * 整数转换为字符串
	 * 
	 * @param appender
	 *            字符序列器
	 * @param val
	 *            整数
	 * @throws IOException
	 */
	static public final void append(Appendable appender, int val) {
		try {
			if (val < 0) {
				// 负数
				val = -val;
				appender.append('-');
			}
			// for (int i = 0; i < _TenStep.length; i++) {
			// if (val >= _TenStep[i]) {
			// appender.append(_TenDigits[(int) (val / _TenStep[i])]);
			// val = val % _TenStep[i];
			// }
			// }

			// int step = 1000000000;
			// while (step > 1) {
			// if (val >= step) {
			// appender.append(_TenDigits[(int) (val / step)]);
			// val = val % step;
			// }
			// step /= 10;
			// }

			if (val >= 10) {
				if (val >= 100) {
					if (val >= 1000) {
						if (val >= 10000) {
							if (val >= 100000) {
								if (val >= 1000000) {
									if (val >= 10000000) {
										if (val >= 100000000) {
											if (val >= 1000000000) {
												appender.append(
														_TenDigits[(int) (val / 1000000000)]);
												val = val % 1000000000;
											}
											appender.append(_TenDigits[(int) (val / 100000000)]);
											val = val % 100000000;
										}
										appender.append(_TenDigits[(int) (val / 10000000)]);
										val = val % 10000000;
									}
									appender.append(_TenDigits[(int) (val / 1000000)]);
									val = val % 1000000;
								}
								appender.append(_TenDigits[(int) (val / 100000)]);
								val = val % 100000;
							}
							appender.append(_TenDigits[(int) (val / 10000)]);
							val = val % 10000;
						}
						appender.append(_TenDigits[(int) (val / 1000)]);
						val = val % 1000;
					}
					appender.append(_TenDigits[(int) (val / 100)]);
					val = val % 100;
				}
				appender.append(_TenDigits[(int) (val / 10)]);
				val = val % 10;
			}

			appender.append(_TenDigits[val]);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * 整数转换为字符串
	 * 
	 * @param appender
	 *            字符序列器
	 * @param val
	 *            整数
	 */
	static public final void append(Appendable appender, long val) {
		try {
			if (val < 0) {
				// 负数
				val = -val;
				appender.append('-');
			}

			if (val >= 10) {
				if (val >= 100) {
					if (val >= 1000) {
						if (val >= 10000) {
							if (val >= 100000) {
								if (val >= 1000000) {
									if (val >= 10000000) {
										if (val >= 100000000) {
											if (val >= 1000000000) {
												if (val >= 10000000000L) {
													if (val >= 100000000000L) {
														if (val >= 1000000000000L) {
															if (val >= 100000000000000L) {
																if (val >= 1000000000000000L) {
																	if (val >= 10000000000000000L) {
																		if (val >= 100000000000000000L) {
																			if (val >= 1000000000000000000L) {
																				appender.append(
																						_TenDigits[(int) (val
																								/ 1000000000000000000L)]);
																				val = val
																						% 1000000000000000000L;
																			}
																			appender.append(
																					_TenDigits[(int) (val
																							/ 100000000000000000L)]);
																			val = val
																					% 100000000000000000L;
																		}
																		appender.append(
																				_TenDigits[(int) (val
																						/ 10000000000000000L)]);
																		val = val
																				% 10000000000000000L;
																	}
																	appender.append(
																			_TenDigits[(int) (val
																					/ 1000000000000000L)]);
																	val = val % 1000000000000000L;
																}
																appender.append(
																		_TenDigits[(int) (val
																				/ 100000000000000L)]);
																val = val % 100000000000000L;
															}
															if (val >= 10000000000000L) {
																appender.append(
																		_TenDigits[(int) (val
																				/ 10000000000000L)]);
																val = val % 10000000000000L;
															}
															appender.append(_TenDigits[(int) (val
																	/ 1000000000000L)]);
															val = val % 1000000000000L;
														}
														appender.append(_TenDigits[(int) (val
																/ 100000000000L)]);
														val = val % 100000000000L;
													}
													appender.append(
															_TenDigits[(int) (val / 10000000000L)]);
													val = val % 10000000000L;
												}
												appender.append(
														_TenDigits[(int) (val / 1000000000)]);
												val = val % 1000000000;
											}
											appender.append(_TenDigits[(int) (val / 100000000)]);
											val = val % 100000000;
										}
										appender.append(_TenDigits[(int) (val / 10000000)]);
										val = val % 10000000;
									}
									appender.append(_TenDigits[(int) (val / 1000000)]);
									val = val % 1000000;
								}
								appender.append(_TenDigits[(int) (val / 100000)]);
								val = val % 100000;
							}
							appender.append(_TenDigits[(int) (val / 10000)]);
							val = val % 10000;
						}
						appender.append(_TenDigits[(int) (val / 1000)]);
						val = val % 1000;
					}
					appender.append(_TenDigits[(int) (val / 100)]);
					val = val % 100;
				}
				appender.append(_TenDigits[(int) (val / 10)]);
				val = val % 10;
			}

			appender.append(_TenDigits[(int) val]);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	static public final void append4(Appendable appender, int val) {
		try {
			if (val >= 10) {
				if (val >= 100) {
					if (val >= 1000) {
						appender.append(_TenDigits[(int) (val / 1000)]);
						val = val % 1000;
					} else {
						appender.append('0');
					}
					appender.append(_TenDigits[(int) (val / 100)]);
					val = val % 100;
				} else {
					appender.append('0');
					appender.append('0');
				}
				appender.append(_TenDigits[(int) (val / 10)]);
				val = val % 10;
			} else {
				appender.append('0');
				appender.append('0');
				appender.append('0');
			}
			appender.append(_TenDigits[val]);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	static public final void append3(Appendable appender, int val) {
		try {
			if (val >= 10) {
				if (val >= 100) {
					appender.append(_TenDigits[(int) (val / 100)]);
					val = val % 100;
				} else {
					appender.append('0');
				}
				appender.append(_TenDigits[(int) (val / 10)]);
				val = val % 10;
			} else {
				appender.append('0');
				appender.append('0');
			}
			appender.append(_TenDigits[val]);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	static public final void append2(Appendable appender, int val) {
		try {
			if (val >= 10) {
				appender.append(_TenDigits[(int) (val / 10)]);
				val = val % 10;
			} else {
				appender.append('0');
			}
			appender.append(_TenDigits[val]);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * 比较两个日期（在当前时区下）是否同一天
	 * 
	 * @param d1
	 *            日期1
	 * @param d2
	 *            日期1
	 * @return 是则返回 true
	 */
	public static boolean isSameDay(Date d1, Date d2) {
		return isSameDay(d1, d2, TIMEZONE_OFFSET);
	}

	/**
	 * 比较两个日期是否同一天
	 * 
	 * @param d1
	 *            日期1
	 * @param d2
	 *            日期2
	 * @param timezone
	 *            时区
	 * @return 是则返回 true
	 */
	public static boolean isSameDay(Date d1, Date d2, int timezone) {
		if (d1 == d2) {
			return true;
		}
		if (null == d1 || null == d2) {
			return false;
		}
		int t1 = (int) ((d1.getTime() + timezone) / DAY_MILLS);
		int t2 = (int) ((d2.getTime() + timezone) / DAY_MILLS);
		return t1 == t2;
	}

	/**
	 * 比较日期前后
	 * 
	 * @param d1
	 *            比较的日期
	 * @param d2
	 *            被比较的日期
	 * @return d1与d2相同返回0，d1早于d2返回-1，d1晚于d2返回1
	 */
	static public int compareTo(Date d1, Date d2) {
		if (d1 == d2) {
			return 0;
		}
		if (null == d1) {
			return -1;
		}
		if (null == d2) {
			return 1;
		}
		long v = d1.getTime() - d2.getTime();
		return (v > 0) ? 1 : ((v < 0) ? -1 : 0);
	}

	/**
	 * 转换日期/时间单位为秒的（自1970年1月1日0时起）整数
	 * 
	 * @param time
	 *            日期/时间
	 * @return 秒为单位的日期时间
	 */
	public final static int getSecondsFromTime(Date time) {
		if (null == time) {
			return 0;
		}
		return (int) (time.getTime() / 1000);
	}

	/**
	 * 转换日期/时间单位为秒的（自1970年1月1日0时起）整数
	 * 
	 * @param time
	 *            系统毫秒为单位的时间，取自Date.getTime()，System.currentTimeMillis()
	 * @return 秒为单位的日期时间
	 */
	public final static int getSecondsFromTime(long time) {
		return (int) (time / 1000);
	}

	/**
	 * 由单位为秒的（自1970年1月1日0时起）整数转为日期/时间对象
	 * 
	 * @param seconds
	 *            秒为单位的日期时间
	 * @return Date对象
	 */
	public final static Date getTimeFromSeconds(int seconds) {
		if (0 == seconds) {
			return GMT1970;
		}
		// long ms;
		// if (seconds < 0) {
		// ms = (0x100000000L + seconds) * 1000;
		// } else {
		// ms = 1000L * seconds;
		// }
		return new Date(((long) seconds) * 1000);
	}

	/**
	 * 由单位为秒的（自1970年1月1日0时起）整数转为日期/时间对象
	 * 
	 * @param seconds
	 *            秒为单位的日期时间
	 * @return Date对象
	 */
	public final static Date getTimeFromSeconds(long seconds) {
		if (0 == seconds) {
			return GMT1970;
		}
		return new Date(seconds * 1000);
	}

	/**
	 * 代替System.currentTimeMillis
	 */
	static public long currentTimeMillis() {
		// TODO 暂时先这样实现
		return System.currentTimeMillis();
	}

	/**
	 * 获取线程安全的日期/时间格式器
	 * 
	 * @param pattern
	 *            格式串，如：yyyyMMddHHmmss，yyyy-MM-dd HH:mm:ss
	 * @return 日期/时间格式器
	 */
	static public DateFormat getDateFormatInstance(String pattern) {
		if (StringUtil.isEmpty(pattern)) {
			throw new NullPointerException("pattern is <null>");
		}
		DateFormatPool df;
		df = _DateFormats.get(pattern);
		if (null == df) {
			df = new DateFormatPool(pattern, null);
			_DateFormats.putIfAbsent(df.getName(), df);
			return df;
		}
		return df;
	}

	/**
	 * 只读取的日期对象，用于静态值的定义
	 * 
	 * @author liangyi
	 * 
	 */
	public static class ReadonlyDate extends Date {
		private static final long serialVersionUID = 1L;

		public ReadonlyDate(long date) {
			super(date);
		}

		@Override
		public void setDate(int date) {
			throw new UnsupportedOperationException("Date object is read-only");
		}

		@Override
		public void setHours(int hours) {
			throw new UnsupportedOperationException("Date object is read-only");
		}

		@Override
		public void setMinutes(int minutes) {
			throw new UnsupportedOperationException("Date object is read-only");
		}

		@Override
		public void setMonth(int month) {
			throw new UnsupportedOperationException("Date object is read-only");
		}

		@Override
		public void setSeconds(int seconds) {
			throw new UnsupportedOperationException("Date object is read-only");
		}

		@Override
		public void setTime(long time) {
			throw new UnsupportedOperationException("Date object is read-only");
		}

		@Override
		public void setYear(int year) {
			throw new UnsupportedOperationException("Date object is read-only");
		}
	}

	/**
	 * 线程安全的日期/时间格式器（使用对象池包装了SimpleDateFormat）
	 * 
	 * @author liangyi
	 *
	 */
	public static class DateFormatPool extends DateFormat implements Nameable {
		private static final long serialVersionUID = 1L;
		protected RingBuffer<SimpleDateFormat> m_Pool;
		protected final String m_Pattern;
		protected final Locale m_Locale;

		public DateFormatPool(String pattern, Locale locale) {
			m_Pattern = pattern;
			m_Locale = locale;
			m_Pool = new RingBuffer<SimpleDateFormat>(32) {
				@Override
				protected SimpleDateFormat onEmpty() {
					SimpleDateFormat df;
					if (null == m_Locale) {
						df = new SimpleDateFormat(m_Pattern);
					} else {
						df = new SimpleDateFormat(m_Pattern, m_Locale);
					}
					return df;
				}
			};
		}

		@Override
		public String getName() {
			return m_Pattern;
		}

		@Override
		public StringBuffer format(Date date, StringBuffer toAppendTo,
				FieldPosition fieldPosition) {
			TimeZone tz = getTimeZone();
			SimpleDateFormat rc = m_Pool.poll();
			try {
				if (null != tz) {
					rc.setTimeZone(tz);
				}
				return rc.format(date, toAppendTo, fieldPosition);
			} finally {
				m_Pool.offer(rc);
			}
		}

		@Override
		public Date parse(String source, ParsePosition pos) {
			TimeZone tz = getTimeZone();
			SimpleDateFormat rc = m_Pool.poll();
			try {
				if (null != tz) {
					rc.setTimeZone(tz);
				}
				return rc.parse(source, pos);
			} finally {
				m_Pool.offer(rc);
			}
		}

		@Override
		public void setCalendar(Calendar newCalendar) {
			throw new UnsupportedOperationException("共享池封装的DateFormat不支持此设置");
		}

		@Override
		public void setLenient(boolean lenient) {
			throw new UnsupportedOperationException("共享池封装的DateFormat不支持此设置");
		}

		@Override
		public void setNumberFormat(NumberFormat newNumberFormat) {
			throw new UnsupportedOperationException("共享池封装的DateFormat不支持此设置");
		}

		@Override
		public void setTimeZone(TimeZone zone) {
			// XXX TimeZone按理也不是线程安全的，但确实没谁有理由去setId或setRawOffset
			if (null == getCalendar()) {
				super.setCalendar(Calendar.getInstance(zone));
				return;
			}
			super.setTimeZone(zone);
		}

		@Override
		public TimeZone getTimeZone() {
			Calendar calendar = getCalendar();
			if (null == calendar) {
				return null;
			}
			return calendar.getTimeZone();
		}
	}

}

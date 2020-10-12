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
package cn.weforward.common.json;

import java.io.IOException;

import cn.weforward.common.crypto.Hex;
import cn.weforward.common.execption.InvalidFormatException;
import cn.weforward.common.util.StringBuilderPool;
import cn.weforward.common.util.StringPool;

/**
 * JSON分析/序列工具
 * 
 * @author liangyi
 *
 */
public class JsonUtil {
	/** 名称池 */
	public static StringPool _NamePool = new StringPool(2048);

	/** 名称缓冲池 */
	protected static StringBuilderPool _NameBuilderPool = new StringBuilderPool(1024, 100);

	/**
	 * 分析过程环境
	 * 
	 * @author liangyi
	 *
	 */
	public static class Context {
		/** 无效残余字符标识 */
		public static final char INVALID_CHAR = 0xffff;

		/** 输入流 */
		private JsonInput in;
		/** 监听器 */
		private Listener listener;
		// /** 名称部分的缓冲区 */
		// private StringBuilder nameBuilder;
		// /** 值部分的缓冲区 */
		// private StringBuilder valueBuilder;
		/** 分析到的层 */
		private int depth;
		/** 由流读取过头的残余字符（若值不为INVALID_CHAR时） */
		private char remainChar;

		public Context(JsonInput in, Listener listener) {
			this.in = in;
			this.listener = (null == listener) ? _unassigned : listener;
			this.remainChar = INVALID_CHAR;
			// nameBuilder = new StringBuilder(100);
			// valueBuilder = new StringBuilder();
		}
	}

	/**
	 * 监听分析事件
	 * 
	 * @author liangyi
	 *
	 */
	public static interface Listener {
		/**
		 * 找到新的JSON节点
		 * 
		 * @param value
		 *            节点值
		 * @param name
		 *            节点名称（若根节点或数组项则为空）
		 * @param depth
		 *            深度（层）
		 * @throws JsonParseAbort
		 *             抛出此异常能中止分析
		 */
		void foundNode(JsonNode value, String name, int depth) throws JsonParseAbort;
	}

	/**
	 * 节点构建器
	 * 
	 * @author zhangpengji
	 *
	 */
	public static interface NodeBuilder {

		JsonObject.Appendable createObject();

		JsonArray.Appendable createArray();

		JsonObject emptyObject();

		JsonArray emptyArray();
	}

	static NodeBuilder DEFAULT_BUILDER = new NodeBuilder() {

		@Override
		public JsonObject.Appendable createObject() {
			return new SimpleJsonObject();
		}

		@Override
		public JsonArray.Appendable createArray() {
			return new SimpleJsonArray();
		}

		public JsonObject emptyObject() {
			return SimpleJsonObject.empty();
		};

		public JsonArray emptyArray() {
			return SimpleJsonArray.empty();
		}
	};

	/**
	 * 不指定监听器时
	 */
	static Listener _unassigned = new Listener() {
		@Override
		public void foundNode(JsonNode value, String name, int depth) {
		}
	};

	/**
	 * 由JSON格式流解析为JsonNode
	 * 
	 * @param in
	 *            格式流
	 * @param listener
	 *            可选的监听器（可以为null）
	 * @return 解析所得的JsonNode
	 * @throws IOException
	 */
	static public JsonNode parse(JsonInput in, Listener listener) throws IOException {
		return parse(in, listener, DEFAULT_BUILDER);
	}

	/**
	 * 由JSON格式流解析为JsonNode
	 * 
	 * @param in
	 *            格式流
	 * @param listener
	 *            可选的监听器（可以为null）
	 * @param builder
	 *            节点构建器
	 * @return 解析所得的JsonNode
	 * @throws IOException
	 */
	static public JsonNode parse(JsonInput in, Listener listener, NodeBuilder builder)
			throws IOException {
		// 找到第一个“{”或“[”符号
		char ch = skipBlank(in, 100);
		JsonNode ret = null;
		if ('{' == ch) {
			// 对象
			Context ctx = new Context(in, listener);
			ret = parseObject(ctx, builder);
		} else if ('[' == ch) {
			// 数组
			Context ctx = new Context(in, listener);
			ret = parseArray(ctx, builder);
		}
		if (null != ret) {
			if (null != listener) {
				listener.foundNode(ret, null, 0);
			}
			return ret;
		}
		throw illegalFormat(in, "JSON格式有误");
	}

	/**
	 * 解析Json对象
	 * 
	 * @param ctx
	 *            分析过程环境
	 * @return 解析得到的JosnObject
	 * @throws IOException
	 */
	/**
	 * 解析Json节点值
	 * 
	 * @param ctx
	 *            分析过程环境
	 * @param name
	 *            节点名称
	 * @return JSON值（字串、数值、布尔、数组或对象）
	 * @throws IOException
	 */
	static private Object parseValue(Context ctx, String name, NodeBuilder builder)
			throws IOException {
		char ch, first;
		StringBuilder valueBuilder = StringBuilderPool._8k.poll();
		try {
			if (Context.INVALID_CHAR != ctx.remainChar) {
				ch = first = ctx.remainChar;
				ctx.remainChar = Context.INVALID_CHAR;
			} else {
				// 跳过空格等
				ch = first = skipBlank(ctx.in, 100);
			}
			// char first = ctx.in.readChar();
			if ('"' == first || '\'' == first) {
				// 由双引号（也兼容不标准的单引号吧）开始的，是字串
				for (;;) {
					ch = ctx.in.readChar();
					if ('\\' == ch) {
						// 转义符，处理转义符
						unescape(ctx.in, valueBuilder);
						continue;
					}
					if (0 != first && first == ch) {
						// 碰到引号结束
						break;
					}
					// if (valueBuilder.length() == valueBuilder.capacity()) {
					// // 超长了？
					// throw illegalFormat(in, "字串太长");
					// }
					valueBuilder.append(ch);
				}
				return valueBuilder.toString();
			}
			if ('{' == first) {
				// 子对象
				JsonObject ret = parseObject(ctx, builder);
				ctx.listener.foundNode(ret, name, ctx.depth);
				return ret;
			}
			if ('[' == first) {
				// 数组
				JsonArray ret = parseArray(ctx, builder);
				ctx.listener.foundNode(ret, name, ctx.depth);
				return ret;
			}
			// 其它类型
			for (;;) {
				if (' ' == ch || '\r' == ch || '\n' == ch) {
					// 空白符（结束）
					break;
				}
				if (',' == ch || '}' == ch || ']' == ch) {
					// 分隔符（也结束），要把它放回残余字符供上游读取
					ctx.remainChar = ch;
					break;
				}
				valueBuilder.append(Character.toLowerCase(ch));
				ch = ctx.in.readChar();
			}
			if (equalsIgnoreCase(valueBuilder, "true")) {
				// true
				return Boolean.TRUE;
			}
			if (equalsIgnoreCase(valueBuilder, "false")) {
				// false
				return Boolean.FALSE;
			}
			if (equalsIgnoreCase(valueBuilder, "null")) {
				return null;
			}

			// 是数值
			String v = valueBuilder.toString();
			if (-1 != v.indexOf('.')) {
				// 有小数位
				return Double.valueOf(v);
			}

			// 整数
			long l = Long.parseLong(v);
			if (l < Integer.MIN_VALUE || l > Integer.MAX_VALUE) {
				return l;
			} else if (l < Short.MIN_VALUE || l > Short.MAX_VALUE) {
				return (int) l;
			} else {
				return (short) l;
			}
		} finally {
			StringBuilderPool._8k.offer(valueBuilder);
		}
	}

	/**
	 * 解析Json对象
	 * 
	 * @param ctx
	 *            分析过程环境
	 * @return 解析得到的JosnObject
	 * @throws IOException
	 */
	static private JsonObject parseObject(Context ctx, NodeBuilder builder) throws IOException {
		char ch;
		ch = skipBlank(ctx.in, 100);
		// 看是否“{}”（没有属性的）空对象
		// ch = ctx.in.readChar();
		if ('}' == ch) {
			// 是“}”，空对象
			return builder.emptyObject();
		}
		++ctx.depth;
		JsonObject.Appendable ret = builder.createObject();
		String name;
		Object value;
		char nameQuot;
		StringBuilder nameBuilder = _NameBuilderPool.poll();
		try {
			while (ctx.in.available() >= 0) {
				nameBuilder.setLength(0);
				// 读取名称，首先看是否有引号（单或双）开首
				nameQuot = ch;
				if ('"' != nameQuot && '\'' != nameQuot) {
					// 没有引号开首
					nameBuilder.append(nameQuot);
					nameQuot = 0;
				}
				for (;;) {
					ch = ctx.in.readChar();
					if ('\\' == ch) {
						// 转义符，处理转义符
						unescape(ctx.in, nameBuilder);
						continue;
					}
					if (nameQuot == ch) {
						// 碰到引号结束
						break;
					} else if (':' == ch && 0 == nameQuot) {
						// 碰到“:”符结束，修整掉sbName后部的空格
						rtrim(nameBuilder);
						break;
					}
					if (nameBuilder.length() == nameBuilder.capacity()) {
						// 名称超长了？
						throw illegalFormat(ctx.in, "名称过长");
					}
					nameBuilder.append(ch);
				}
				// name = pool(nameBuilder.toString());
				// 池化合并名称字串减少内存使用
				name = _NamePool.intern(nameBuilder);
				// 找到“:”分隔符
				while (':' != ch) {
					ch = ctx.in.readChar();
				}
				// ctx.valueBuilder.setLength(0);
				value = parseValue(ctx, name, builder);
				// 对象属性
				ret.add(name, value);
				if (Context.INVALID_CHAR != ctx.remainChar) {
					ch = ctx.remainChar;
					ctx.remainChar = Context.INVALID_CHAR;
				} else {
					// 跳过空格等
					ch = skipBlank(ctx.in, 100);
				}
				// ch = ctx.in.readChar();
				if (',' == ch) {
					// 下个兄弟节点
					ch = skipBlank(ctx.in, 100);
					continue;
				}

				if ('}' == ch) {
					// 对象结束
					break;
				}

				if (']' == ch) {
					// 数组结束，但这应该是对象啊
					throw illegalFormat(ctx.in, "不是预期的']'");
				}
			}
		} finally {
			_NameBuilderPool.offer(nameBuilder);
		}
		--ctx.depth;
		return ret;
	}

	/**
	 * 解析Json数组
	 * 
	 * @param ctx
	 *            分析过程环境
	 * @return 解析得到的JosnArray
	 * @throws IOException
	 */
	static private JsonArray parseArray(Context ctx, NodeBuilder builder) throws IOException {
		char ch;
		ch = skipBlank(ctx.in, 100);
		// 看是否“[]”空数组
		// ch = ctx.in.readChar();
		if (']' == ch) {
			return builder.emptyArray();
		}
		ctx.remainChar = ch;
		JsonArray.Appendable ret = builder.createArray();
		Object value;
		++ctx.depth;
		while (ctx.in.available() >= 0) {
			// ctx.valueBuilder.setLength(0);
			value = parseValue(ctx, null, builder);
			// 数组项
			ret.add(value);
			if (Context.INVALID_CHAR != ctx.remainChar) {
				ch = ctx.remainChar;
				ctx.remainChar = Context.INVALID_CHAR;
			} else {
				// 跳过空格等
				ch = skipBlank(ctx.in, 100);
			}
			if (',' == ch) {
				// 下个兄弟节点
				continue;
			}

			if (']' == ch) {
				// 数组结束
				break;
			}

			if ('}' == ch) {
				// 对象结束，但这应该是数组啊
				throw illegalFormat(ctx.in, "不是预期的'}'");
			}
		}
		--ctx.depth;
		return ret;
	}

	public static void format(JsonNode json, JsonOutput out) throws IOException {
		if (json instanceof JsonObject) {
			formatObject((JsonObject) json, out);
			return;
		}

		// if (json instanceof JsonArray) {
		formatArray((JsonArray) json, out);
		// return;
		// }
	}

	static private void formatObject(JsonObject object, JsonOutput out) throws IOException {
		out.append('{');
		boolean first = true;
		for (JsonPair pair : object.items()) {
			if (first) {
				first = false;
			} else {
				out.append(',');
			}
			out.append('"');
			escape(pair.getKey(), out);
			out.append('"');
			out.append(':');
			formatValue(pair.getValue(), out);
		}
		out.append('}');
	}

	static private void formatArray(JsonArray array, JsonOutput out) throws IOException {
		out.append('[');
		int count = array.size();
		for (int i = 0; i < count; i++) {
			if (i > 0) {
				out.append(',');
			}
			formatValue(array.item(i), out);
		}
		out.append(']');
	}

	static private void formatValue(Object value, JsonOutput out) throws IOException {
		if (null == value) {
			out.append("null");
			return;
		}
		if (value instanceof String) {
			out.append('"');
			escape((String) value, out);
			out.append('"');
			return;
		}
		if (value instanceof Number) {
			Number v = (Number) value;
			out.append(v.toString());
			return;
		}
		if (value instanceof JsonObject) {
			formatObject((JsonObject) value, out);
			return;
		}
		if (value instanceof JsonArray) {
			formatArray((JsonArray) value, out);
			return;
		}
		if (value instanceof Boolean) {
			Boolean v = (Boolean) value;
			if (v.booleanValue()) {
				out.append("true");
			} else {
				out.append("false");
			}
			return;
		}
		throw new InvalidFormatException("JSON值类型不支持" + value);
	}

	/**
	 * 转换为小写
	 * 
	 * @param ch
	 *            字符
	 * @return 小写字符
	 */
	static private char toLowerCase(char ch) {
		if (ch <= 'Z' && ch >= 'A') {
			return (char) (ch + 32);
		}
		return ch;
	}

	/**
	 * 忽略大小写比较
	 * 
	 * @param str1
	 *            比较字串
	 * @param str2
	 *            被比较字串
	 * @return 是否相同
	 */
	static public boolean equalsIgnoreCase(CharSequence str1, CharSequence str2) {
		if (null == str1 || null == str2) {
			return false;
		}
		if (str1 == str2) {
			return true;
		}
		int len = str1.length();
		if (str2.length() != len) {
			return false;
		}
		char ch1, ch2;
		for (int i = 0; i < len; i++) {
			ch1 = toLowerCase(str1.charAt(i));
			ch2 = toLowerCase(str2.charAt(i));
			if (ch1 != ch2) {
				return false;
			}
		}
		return true;
	}

	/**
	 * 去除后部空格
	 * 
	 * @param builder
	 *            要去除后空格的字串
	 */
	static public void rtrim(StringBuilder builder) {
		int i = builder.length() - 1;
		while (i >= 0 && isBlank(builder.charAt(i))) {
			i--;
		}
		builder.setLength(i + 1);
	}

	/**
	 * 跳过空格、换行符等
	 * 
	 * @param in
	 *            JSON格式流
	 * @param limit
	 *            限制跳过的字符数
	 * @throws InvalidFormatException
	 */
	static public char skipBlank(JsonInput in, int limit) throws IOException {
		char ch;
		do {
			if (0 == (--limit)) {
				throw illegalFormat(in, "空格及换行太多");
			}
			ch = in.readChar();
		} while (isBlank(ch));
		return ch;
	}

	/**
	 * 是否空白字符（空格、TAB、换行符等）
	 * 
	 * @param ch
	 *            要检查的字符
	 */
	final public static boolean isBlank(char ch) {
		return ' ' == ch || '\t' == ch || '\r' == ch || '\n' == ch;
	}

	/**
	 * 格式无效异常
	 * 
	 * @param in
	 *            格式串
	 * @param pos
	 *            位置
	 * @return InvalidFormatException异常
	 */
	private static InvalidFormatException illegalFormat(JsonInput in, String errorMsg) {
		// if (in.length() > (50 + pos)) {
		// msg = "JSON illegal format[offset=" + pos + "]" + in.substring(pos,
		// pos + 50) + "...";
		// } else {
		// msg = "JSON illegal format[offset=" + pos + "]" + in.substring(pos);
		// }
		return new InvalidFormatException(errorMsg + " " + in);
	}

	private static boolean isEmpty(CharSequence string) {
		return null == string || 0 == string.length();
	}

	/**
	 * 编码JOSN字串
	 * 
	 * any-Unicode-character-except-"-or-\-or- control-character<br/>
	 * \" \\ \/ \b \f \n \r \t \ u four-hex-digits<br/>
	 * <br/>
	 * char = unescaped /<br/>
	 * escape (<br/>
	 * %x22 / ; " quotation mark U+0022<br/>
	 * %x5C / ; \ reverse solidus U+005C<br/>
	 * %x2F / ; / solidus U+002F<br/>
	 * %x62 / ; b backspace U+0008<br/>
	 * %x66 / ; f form feed U+000C<br/>
	 * %x6E / ; n line feed U+000A<br/>
	 * %x72 / ; r carriage return U+000D<br/>
	 * %x74 / ; t tab U+0009<br/>
	 * %x75 4HEXDIG ) ; uXXXX U+XXXX<br/>
	 * escape = %x5C ; \<br/>
	 * quotation-mark = %x22 ; "<br/>
	 * unescaped = %x20-21 / %x23-5B / %x5D-10FFFF<br/>
	 * 
	 * <url>http://www.ietf.org/rfc/rfc4627.txt?number=4627</url>
	 * 
	 * @param string
	 *            要转义的字串
	 * @param appender
	 *            转义后输出的序列器
	 */
	static public void escape(CharSequence string, Appendable appender) throws IOException {
		if (isEmpty(string)) {
			return;
		}
		for (int i = 0; i < string.length(); i++) {
			char ch = string.charAt(i);
			switch (ch) {
			case '"':
				appender.append("\\\"");
				break;
			case '\\':
				appender.append("\\\\");
				break;
			case '/':
				appender.append("\\/");
				break;
			case 0x08:
				appender.append("\\b");
				break;
			case 0x0c:
				appender.append("\\f");
				break;
			case '\n':
				appender.append("\\n");
				break;
			case '\r':
				appender.append("\\r");
				break;
			case '\t':
				appender.append("\\t");
				break;
			default:
				if (0x20 == ch || 0x21 == ch || (ch >= 0x23 && ch <= 0x5B) || ch >= 0x5D
						|| ch < 0) {
					appender.append(ch);
				} else {
					// \\uxxxx编码
					appender.append("\\u");
					Hex.toHexFixed((short) ch, appender);
				}
				break;
			}
		}
	}

	static public Appendable unescape(JsonInput in, Appendable builder) throws IOException {
		char ch = in.readChar();
		switch (ch) {
		case '"':
		case '\\':
		case '/':
			builder.append(ch);
			break;
		case 'b':
			// sb.append('\b');
			builder.append((char) 0x08);
			break;
		case 'f':
			// sb.append('\f');
			builder.append((char) 0x0c);
			break;
		case 'n':
			builder.append('\n');
			break;
		case 'r':
			builder.append('\r');
			break;
		case 't':
			builder.append('\t');
			break;
		case 'u':
			// \\uxxx
			StringBuilder unicode = new StringBuilder(4);
			unicode.append(in.readChar());
			unicode.append(in.readChar());
			unicode.append(in.readChar());
			unicode.append(in.readChar());
			// if (i + 4 >= in.length()) {
			// // 格式有问题，当是“\\u”字符算了
			// sb.append("\\u");
			// break;
			// }
			// FIXME 解析UNICODE码
			int code = Hex.parseHex(unicode, 4, Integer.MAX_VALUE);
			if (Integer.MAX_VALUE == code) {
				// 格式有误，当是“\\u”字符算了
				builder.append("\\u");
			} else {
				builder.append((char) code);
			}
			break;
		default:
			// 格式有问题，当是“\x”字符算了
			builder.append('\n').append(ch);
			break;
		}
		return builder;
	}

}

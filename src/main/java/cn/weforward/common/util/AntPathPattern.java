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

import java.nio.file.InvalidPathException;
import java.util.regex.Pattern;

/**
 * Ant风格的模式匹配
 * 
 * @author zhangpengji
 *
 */
public class AntPathPattern {

	static final char SEPARATOR = '/';
	static final String[] EMPTY_STRING_ARRAY = new String[0];
	static final Pattern[] EMPTY_PATTERN_ARRAY = new Pattern[0];
	static final char[] WILDCARD_CHARS = { '*', '?' };

	/** 匹配的URI最大的目录层数 */
	static final int MAX_TOKENIZE_SLICE = 50;

	// private String m_Pattern;
	private char m_Separator;
	// private boolean m_Compile;

	private boolean m_StartsWithSeparator;
	private boolean m_EndsWithSeparator;
	// 由pattern分解得到的dir
	private String[] m_PattDirs;
	// 非**的dir正则匹配模式，与m_PattDirs一一对应，“**”，“*”，“?”使用null占位
	private Pattern[] m_DirPatterns;

	/**
	 * 用于实现单例
	 * 
	 * @author liangyi
	 */
	private static class Singleton {
		static Tokenizer _emptyTokenizer = new Tokenizer();
		/** Tokenizer池 */
		static RingBuffer<Tokenizer> _tokenizerPool = new RingBuffer<Tokenizer>(128) {
			@Override
			protected Tokenizer onEmpty() {
				return new Tokenizer();
			}

			@Override
			public boolean offer(Tokenizer item) {
				if (null == item || _emptyTokenizer == item) {
					// 略过
					return false;
				}
				item.cleanup();
				return super.offer(item);
			}
		};
	}

	private AntPathPattern(String pattern, char pathSeparator) {
		if (null == pattern) {
			throw new IllegalArgumentException("'pattern' is required");
		}
		m_Separator = pathSeparator;
		compile(pattern);
	}

	public static AntPathPattern valueOf(String pattern) {
		return valueOf(pattern, SEPARATOR);
	}

	public static AntPathPattern valueOf(String pattern, char pathSeparator) {
		// XXX 加入缓存？
		return new AntPathPattern(pattern, pathSeparator);
	}

	public static boolean match(String pattern, String path) {
		AntPathPattern ant = valueOf(pattern);
		return ant.match(path);
	}

	public static boolean matchStart(String pattern, String path) {
		AntPathPattern ant = valueOf(pattern);
		return ant.matchStart(path);
	}

	private void compile(String pattern) {
		if (0 == pattern.length()) {
			m_StartsWithSeparator = false;
			m_EndsWithSeparator = false;
			m_PattDirs = EMPTY_STRING_ARRAY;
			m_DirPatterns = EMPTY_PATTERN_ARRAY;
		} else {
			m_StartsWithSeparator = startsWith(pattern, m_Separator);
			m_EndsWithSeparator = endsWith(pattern, m_Separator);
			// List<String> ss = tokenizePath_(pattern, m_Separator);
			Tokenizer tokenizer = tokenizePath(pattern, m_Separator);
			m_PattDirs = tokenizer.size() > 0 ? tokenizer.toArray(new String[tokenizer.size()])
					: EMPTY_STRING_ARRAY;
			Singleton._tokenizerPool.offer(tokenizer);
			m_DirPatterns = new Pattern[m_PattDirs.length];
			for (int i = 0; i < m_PattDirs.length; i++) {
				String dir = m_PattDirs[i];
				if ("**".equals(dir) || "*".equals(dir) || "?".equals(dir)) {
					m_DirPatterns[i] = null;
				} else {
					m_DirPatterns[i] = toRegexPattern(dir);
				}
			}
		}
		// m_Compile = true;
	}

	static Pattern toRegexPattern(String antPattern) {
		StringBuilder sb = new StringBuilder();
		int bi = 0;
		for (int ei = 0; ei < antPattern.length(); ei++) {
			char c = antPattern.charAt(ei);
			if ('?' == c || '*' == c) {
				if (ei > bi) {
					sb.append(Pattern.quote(antPattern.substring(bi, ei)));
				}
				if ('?' == c) {
					sb.append('.');
				} else {
					sb.append('.').append('*');
				}
				bi = ei + 1;
			}
		}
		if (bi < antPattern.length()) {
			sb.append(Pattern.quote(antPattern.substring(bi)));
		}
		return Pattern.compile(sb.toString());
	}

	static boolean startsWith(String str, char ch) {
		if (0 == str.length()) {
			return false;
		}
		return ch == str.charAt(0);
	}

	static boolean endsWith(String str, char ch) {
		if (0 == str.length()) {
			return false;
		}
		return ch == str.charAt(str.length() - 1);
	}

	/**
	 * 按分隔符的分割路径
	 * 
	 * @author liangyi
	 *
	 */
	public static class Tokenizer {
		/** 要分割的字串 */
		CharSequence string;
		/** 按分隔符的分割位 */
		int indexs[];
		/** 分割数 */
		int size;

		public Tokenizer() {
		}

		public String[] toArray(String[] strings) {
			if (size() != strings.length) {
				strings = (String[]) java.lang.reflect.Array
						.newInstance(strings.getClass().getComponentType(), size());
			}
			for (int i = size() - 1; i >= 0; i--) {
				strings[i] = slice(i).toString();
			}
			return strings;
		}

		public void tokenize(CharSequence string, char separator) {
			this.string = string;
			this.size = 1;
			if (null == indexs) {
				indexs = new int[MAX_TOKENIZE_SLICE + 1];
			}
			int length = string.length();
			if (string.charAt(0) == separator) {
				// 第一个字符就是分隔符
				indexs[0] = 0;
			} else {
				indexs[0] = -1;
			}
			int i;
			for (i = 1; i < length; i++) {
				if (string.charAt(i) == separator) {
					if (size >= indexs.length) {
						throw new InvalidPathException(string.toString(), "路径分割数超过" + size);
					}
					indexs[size++] = i;
				}
			}
			if (indexs[size - 1] + 1 < string.length()) {
				if (size >= indexs.length) {
					throw new InvalidPathException(string.toString(), "路径分割数超过" + size);
				}
				indexs[size++] = string.length();
			}
		}

		public void cleanup() {
			size = 0;
			string = null;
		}

		public int size() {
			return this.size - 1;
		}

		public CharSequence slice(int index) throws IndexOutOfBoundsException {
			if (index < 0 || index + 2 > size) {
				throw new IndexOutOfBoundsException(index + " over 0~" + (size - 2));
			}
			return StringUtil.subSequence(string, indexs[index] + 1, indexs[index + 1]);
		}
	}

	static Tokenizer tokenizePath(String path, char separator) {
		if (StringUtil.isEmpty(path)) {
			return Singleton._emptyTokenizer;
		}

		// Tokenizer tokenizer = new Tokenizer();
		Tokenizer tokenizer = Singleton._tokenizerPool.poll();
		tokenizer.tokenize(path, separator);
		return tokenizer;
	}

	// /**
	// * 将path按分隔符分解为多个dir
	// *
	// * @param path
	// * @param separator
	// * @return
	// */
	// static List<String> tokenizePath_(String path, char separator) {
	// if (StringUtil.isEmpty(path)) {
	// // return EMPTY_STRING_ARRAY;
	// return Collections.emptyList();
	// }
	// if (1 == path.length()) {
	// if (separator != path.charAt(0)) {
	// // return new String[] { path };
	// return Collections.singletonList(path);
	// } else {
	// // return EMPTY_STRING_ARRAY;
	// return Collections.emptyList();
	// }
	// }
	// List<String> tokens = new ArrayList<>();
	// int bi = 0;
	// for (int ei = 0; ei < path.length(); ei++) {
	// char c = path.charAt(ei);
	// if (separator == c) {
	// if (ei > bi) {
	// tokens.add(path.substring(bi, ei));
	// }
	// bi = ei + 1;
	// }
	// }
	// if (bi < path.length()) {
	// tokens.add(path.substring(bi));
	// }
	// if (0 == tokens.size()) {
	// // return EMPTY_STRING_ARRAY;
	// return Collections.emptyList();
	// }
	// // return tokens.toArray(new String[tokens.size()]);
	// return tokens;
	// }

	/**
	 * 判断传入的<code>path</code>是否匹配
	 * 
	 * @param path
	 * @return
	 */
	public boolean match(String path) {
		return match0(path, false);
	}

	/**
	 * 判断传入的<code>path</code>是否符合前缀匹配
	 * 
	 * @param path
	 * @return
	 */
	public boolean matchStart(String path) {
		return match0(path, true);
	}

	private boolean match0(String path, boolean startMatch) {
		// if (!m_Compile) {
		// synchronized (this) {
		// if (!m_Compile) {
		// compile();
		// }
		// }
		// }

		if (m_StartsWithSeparator != startsWith(path, m_Separator)) {
			return false;
		}

		if (!startMatch && !isRoughlyMatch(path)) {
			return false;
		}

		String[] pattDirs = m_PattDirs;
		int pattIdxStart = 0;
		int pattIdxEnd = pattDirs.length - 1;
		int pathIdxStart = 0;
		int pathIdxEnd;
		Tokenizer pathDirs = null;
		try {
			pathDirs = tokenizePath(path, m_Separator);
			pathIdxEnd = pathDirs.size() - 1;

			// 匹配所有dir，直到遇到“**”
			while (pattIdxStart <= pattIdxEnd && pathIdxStart <= pathIdxEnd) {
				String pattDir = pattDirs[pattIdxStart];
				if ("**".equals(pattDir)) {
					break;
				}
				if (!matchStrings(pattIdxStart, pathDirs.slice(pathIdxStart))) {
					return false;
				}
				pattIdxStart++;
				pathIdxStart++;
			}

			if (pathIdxStart > pathIdxEnd) {
				// path的dir已用尽
				if (pattIdxStart > pattIdxEnd) {
					// pattern也用尽
					return (m_EndsWithSeparator == endsWith(path, m_Separator));
				}
				if (startMatch) {
					// pattern未用尽，前缀匹配成功
					return true;
				}
				if (pattIdxStart == pattIdxEnd && pattDirs[pattIdxStart].equals("*")
						&& endsWith(path, m_Separator)) {
					// path已用尽，且以分隔符结尾，且pattern剩最后一个“*”
					return true;
				}
				for (int i = pattIdxStart; i <= pattIdxEnd; i++) {
					if (!pattDirs[i].equals("**")) {
						return false;
					}
				}
				return true;
			} else if (pattIdxStart > pattIdxEnd) {
				// pattern用尽了，path还没。匹配失败
				return false;
			} else if (startMatch && "**".equals(pattDirs[pattIdxStart])) {
				// 有“**”的情况下，前缀匹配必定成功
				return true;
			}

			// 找到最后一个“**”
			while (pattIdxStart <= pattIdxEnd && pathIdxStart <= pathIdxEnd) {
				String pattDir = pattDirs[pattIdxEnd];
				if (pattDir.equals("**")) {
					break;
				}
				if (!matchStrings(pattIdxEnd, pathDirs.slice(pathIdxEnd))) {
					return false;
				}
				pattIdxEnd--;
				pathIdxEnd--;
			}
			if (pathIdxStart > pathIdxEnd) {
				// path已用尽
				for (int i = pattIdxStart; i <= pattIdxEnd; i++) {
					if (!pattDirs[i].equals("**")) {
						return false;
					}
				}
				return true;
			}

			while (pattIdxStart != pattIdxEnd && pathIdxStart <= pathIdxEnd) {
				int patIdxTmp = -1;
				for (int i = pattIdxStart + 1; i <= pattIdxEnd; i++) {
					if (pattDirs[i].equals("**")) {
						patIdxTmp = i;
						break;
					}
				}
				if (patIdxTmp == pattIdxStart + 1) {
					// “**/**”的情况，跳过一个
					pattIdxStart++;
					continue;
				}
				int patLength = (patIdxTmp - pattIdxStart - 1);
				int strLength = (pathIdxEnd - pathIdxStart + 1);
				int foundIdx = -1;

				strLoop: for (int i = 0; i <= strLength - patLength; i++) {
					for (int j = 0; j < patLength; j++) {
						int subPatIdx = pattIdxStart + j + 1;
						int subStrIdx = pathIdxStart + i + j;
						if (!matchStrings(subPatIdx, pathDirs.slice(subStrIdx))) {
							continue strLoop;
						}
					}
					foundIdx = pathIdxStart + i;
					break;
				}

				if (foundIdx == -1) {
					return false;
				}

				pattIdxStart = patIdxTmp;
				pathIdxStart = foundIdx + patLength;
			}
		} finally {
			Singleton._tokenizerPool.offer(pathDirs);
		}

		for (int i = pattIdxStart; i <= pattIdxEnd; i++) {
			if (!pattDirs[i].equals("**")) {
				return false;
			}
		}
		return true;
	}

	/**
	 * 粗略判断下是否匹配
	 * 
	 * @param path
	 * @return
	 */
	private boolean isRoughlyMatch(String path) {
		int pos = 0;
		for (String pattDir : m_PattDirs) {
			int skipped = skipSeparator(path, pos);
			pos += skipped;
			skipped = skipDir(path, pos, pattDir);
			if (skipped < pattDir.length()) {
				// 不能完全匹配此dir，看下是否存在部分匹配或者存在通配符
				return (skipped > 0 || (pattDir.length() > 0 && isWildcardChar(pattDir.charAt(0))));
			}
			pos += skipped;
		}
		return true;
	}

	/**
	 * 跳过最大可能的匹配
	 * 
	 * @param path
	 * @param pos
	 * @param dir
	 * @return
	 */
	private int skipDir(String path, int pos, String dir) {
		int skipped = 0;
		for (int i = 0; i < dir.length(); i++) {
			char c = dir.charAt(i);
			if (isWildcardChar(c)) {
				return skipped;
			}
			int currPos = pos + skipped;
			if (currPos >= path.length()) {
				return 0;
			}
			if (c == path.charAt(currPos)) {
				skipped++;
			}
		}
		return skipped;
	}

	/**
	 * 跳过分隔符，找到第一个非分隔符的位置
	 * 
	 * @param path
	 * @param pos
	 * @return
	 */
	private int skipSeparator(String path, int pos) {
		if (pos >= path.length()) {
			return 0;
		}
		int skipped = 0;
		while (m_Separator == path.charAt(pos + skipped)) {
			skipped++;
			if ((pos + skipped) >= path.length()) {
				break;
			}
		}
		return skipped;
	}

	private boolean isWildcardChar(char c) {
		for (char candidate : WILDCARD_CHARS) {
			if (c == candidate) {
				return true;
			}
		}
		return false;
	}

	private boolean matchStrings(int subPatIdx, CharSequence charSequence) {
		// ** 不会进入此方法
		// if ("**".equals(m_PattDirs[subPatIdx])) {
		// return true;
		// }
		if ("*".equals(m_PattDirs[subPatIdx])) {
			return true;
		}
		if ("?".equals(m_PattDirs[subPatIdx])) {
			return 1 == charSequence.length();
		}
		return m_DirPatterns[subPatIdx].matcher(charSequence).matches();
	}
}

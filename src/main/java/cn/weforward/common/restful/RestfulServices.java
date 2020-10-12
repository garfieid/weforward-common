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
package cn.weforward.common.restful;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.weforward.common.execption.DuplicateNameException;
import cn.weforward.common.util.UriMatcher;

/**
 * RESTful API集的支持
 * 
 * @author liangyi
 * 
 */
public class RestfulServices implements RestfulService {
	protected Map<String, RestfulService> m_Services;
	protected UriMatcher m_UriMatcher;
	protected RestfulAuthorizer m_Authorizer;

	public void setServices(Map<String, RestfulService> services) {
		HashMap<String, RestfulService> map = new HashMap<String, RestfulService>(services.size());
		RestfulService old;
		List<String> wildcards = null;
		String uri;
		for (Map.Entry<String, RestfulService> e : services.entrySet()) {
			uri = e.getKey().toLowerCase();
			old = map.put(uri, e.getValue());
			if (null != old) {
				if (old.equals(e.getValue())) {
					continue;
				}
				throw new DuplicateNameException("URI重复：" + uri);
			}
			if (uri.indexOf('*') >= 0) {
				// 有通配符
				if (null == wildcards) {
					wildcards = new ArrayList<String>();
				}
				wildcards.add(uri);
			}
		}
		if (null != wildcards) {
			UriMatcher matcher = m_UriMatcher;
			if (null == matcher) {
				matcher = new UriMatcher();
			}
			matcher.setUris(wildcards);
			m_UriMatcher = matcher;
		}
		m_Services = map;
	}

	public void setAuthorizer(RestfulAuthorizer authorizer) {
		m_Authorizer = authorizer;
	}

	/**
	 * 匹配最合适URI的RestfulService
	 * 
	 * @param uri
	 *            URI
	 * @return 匹配的RestfulService，返回null为无匹配
	 */
	public RestfulService matchService(String uri) {
		Map<String, RestfulService> services = m_Services;
		if (null == services || services.isEmpty()) {
			return null;
		}
		// 先在绝对URI表匹配
		RestfulService service = services.get(uri);
		if (null != service) {
			return service;
		}
		// 没有则再试在有通配符的表匹配
		UriMatcher matcher = m_UriMatcher;
		if (null != matcher) {
			String match = matcher.match(uri);
			if (null != match) {
				service = services.get(match);
				return service;
			}
		}
		return null;
	}

	public void precheck(RestfulRequest request, RestfulResponse response) throws IOException {
		if (null != m_Authorizer) {
			String accessId = m_Authorizer.auth(request);
			if (null == accessId) {
				// 拒绝访问
				response.setStatus(RestfulResponse.STATUS_FORBIDDEN);
				response.openOutput().close();
				return;
			}
		}
	}

	public void service(RestfulRequest request, RestfulResponse response) throws IOException {
		String uri = request.getUri();
		if (null != uri && uri.length() > 0) {
			uri = uri.toLowerCase();
		}
		RestfulService service = matchService(uri);
		if (null == service) {
			// 无匹配404
			response.setStatus(RestfulResponse.STATUS_NOT_FOUND);
			response.openOutput().close();
			return;
		}
		service.service(request, response);
	}

	@Override
	public void timeout(RestfulRequest request, RestfulResponse response) throws IOException {
		String uri = request.getUri();
		if (null != uri && uri.length() > 0) {
			uri = uri.toLowerCase();
		}
		RestfulService service = matchService(uri);
		if (null == service) {
			// 无匹配404
			response.setStatus(RestfulResponse.STATUS_NOT_FOUND);
			response.openOutput().close();
			return;
		}
		service.timeout(request, response);
	}
}

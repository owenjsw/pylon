package com.yotouch.base.util;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.yotouch.core.Consts;
import com.yotouch.core.entity.Entity;
import com.yotouch.core.entity.MetaEntity;
import com.yotouch.core.entity.MetaField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class WebUtil {

    static final private Logger logger = LoggerFactory.getLogger(WebUtil.class);

    @Value("${app.host:}")
    private String appHost;

    public String getThemeTpl(String prefix, String theme, String file) {
        String s = prefix + "/theme/" + theme + "/" + file;

        logger.debug("themetpl " + s);

        return s;
    }

    public String getThemeTpl(String prefix, HttpServletRequest request, String file) {
        return this.getThemeTpl(prefix, (String) request.getAttribute("theme"), file);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Map<String, Object> asRetJson(Map<String, Object> ret) {

        Map<String, Object> newMap = new HashMap<>();

        for (String key : ret.keySet()) {

            //logger.info("Parse to ret json key " + key);

            Object value = ret.get(key);

            if (value instanceof List) {
                newMap.put(key, parseList((List) value));
            } else if (value instanceof Map) {
                newMap.put(key, asRetJson((Map<String, Object>) value));
            } else if (value instanceof Entity) {
                Entity e = (Entity) value;
                newMap.put(key, e.valueMap());
            } else {
                newMap.put(key, value);
            }

        }

        return newMap;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private List parseList(List l) {
        List newList = new ArrayList<>();

        for (Object o : l) {
            if (o instanceof List) {
                newList.add(parseList((List) o));
            } else if (o instanceof Map) {
                newList.add(asRetJson((Map<String, Object>) o));
            } else if (o instanceof Entity) {
                Entity e = (Entity) o;
                newList.add(e.valueMap());
            } else {
                newList.add(o);
            }
        }

        return newList;

    }

    

    private void setRequestValue(HttpServletRequest request, Entity e, MetaField<?> mf) {
        String name = e.getMetaEntity().getName() + "_" + mf.getName();
        String value = request.getParameter(name);
        
        //logger.info("Set http value for entity " + name + " value " + value + " mf type " + mf.getDataType());
        
        if (mf.isMultiReference()) {
            String uuids = "";
            if (!StringUtils.isEmpty(value)) {
                uuids = value;
            }
            
            List<String> uuidList = Lists.newArrayList(Splitter.on(',').omitEmptyStrings().split(uuids));
            
            //logger.info("MR value " + uuidList);
            
            if (uuidList == null || uuidList.size() == 0) {
                e.setValue(mf.getName(), new ArrayList<String>());
            } else {
                e.setValue(mf.getName(), uuidList);
            }
        } else if (mf.isSingleReference()) {
            e.setValue(mf.getName(), request.getParameter(name));
        } else if (Consts.META_FIELD_TYPE_DATA_FIELD.equalsIgnoreCase(mf.getFieldType())) {
            if (Consts.META_FIELD_DATA_TYPE_INT.equals(mf.getDataType())
                    || Consts.META_FIELD_DATA_TYPE_DOUBLE.equals(mf.getDataType())) {
                if (StringUtils.isEmpty(value)) {
                    return;
                }
            }
            e.setValue(mf.getName(), value);
        }
    }

    public String getFullUrl(HttpServletRequest request) {
        String requestURL = request.getRequestURI();
        String queryString = request.getQueryString();

        String url = "";
        if (queryString == null) {
            url = appHost + requestURL.toString();
        } else {
            url = appHost + requestURL + "?" + queryString;
        }

        if (!url.startsWith("http://")) {
            url = "http://" + url;
        }

        return url;
    }

    public Entity updateEntityVariables(Entity entity, HttpServletRequest request) {
        
        Map<String, String[]> paramMap = request.getParameterMap();
        
        MetaEntity me = entity.getMetaEntity();
        for (MetaField<?> mf: me.getMetaFields()) {
            String fname = me.getName() + "_" + mf.getName();
            if (!paramMap.containsKey(fname)) {
                continue;
            }
            
            this.setRequestValue(request, entity, mf);            
        }
        
        return entity;
    }

    public String getBaseUrl(HttpServletRequest request) {
        if (request.getServerPort() == 80) {
            return String.format("%s://%s",request.getScheme(),  this.appHost);
        } else {
            return String.format("%s://%s:%d",request.getScheme(), this.appHost, request.getServerPort());
        }
    }

    public String getBaseUrl() {
        return "http://" + this.appHost;
    }

    public String getClientId(HttpServletRequest request){
        String bid = "";
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie c : cookies) {
                if ("_cid".equalsIgnoreCase(c.getName())) {
                    bid = c.getValue();
                }
            }
        }

        if (StringUtils.isEmpty(bid)) {
            return "";
        } else {
            return bid.trim();
        }
    }


    public boolean isIOS(HttpServletRequest request) {
        String ua = this.getUserAgent(request);

        return ua.toLowerCase().contains("iphone") || ua.toLowerCase().contains("ipad");
    }

    public boolean isAndroid(HttpServletRequest request) {

        String ua = this.getUserAgent(request);

        return ua.toLowerCase().contains("android");

    }

    public String getUserAgent(HttpServletRequest request) {

        String userAgent = request.getHeader("User-Agent");

        return userAgent;

    }

    public boolean isWechat(HttpServletRequest request) {
        // Mozilla/5.0 (iPhone; CPU iPhone OS 8_3 like Mac OS X) AppleWebKit/600.1.4 (KHTML, like Gecko) Mobile/12F70 MicroMessenger/6.2.6 NetType/WIFI Language/zh_CN

        String userAgent = request.getHeader("User-Agent");

        //logger.info("UserAgent " + userAgent);

        if (userAgent == null) {
            return false;
        }

        return userAgent.toLowerCase().contains("micromessenger");
    }

    public String getClientType(HttpServletRequest request){
        String userAgent = request.getHeader("User-Agent");

        if (userAgent != null && userAgent.toLowerCase().indexOf("android") != -1){
            return "android";
        }

        if (userAgent != null && userAgent.toLowerCase().indexOf("iphone") != -1){
            return "iphone";
        }

        return "unknown";
    }
}

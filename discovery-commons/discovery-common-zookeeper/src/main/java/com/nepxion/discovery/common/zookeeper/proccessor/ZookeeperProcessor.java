package com.nepxion.discovery.common.zookeeper.proccessor;

/**
 * <p>Title: Nepxion Discovery</p>
 * <p>Description: Nepxion Discovery</p>
 * <p>Copyright: Copyright (c) 2017-2050</p>
 * <p>Company: Nepxion</p>
 * @author rotten
 * @version 1.0
 */

import javax.annotation.PostConstruct;

import org.apache.curator.framework.recipes.cache.TreeCacheListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;

import com.nepxion.discovery.common.zookeeper.constant.ZookeeperConstant;
import com.nepxion.discovery.common.zookeeper.operation.ZookeeperOperation;
import com.nepxion.discovery.common.zookeeper.operation.ZookeeperSubscribeCallback;

public abstract class ZookeeperProcessor implements DisposableBean {
    private static final Logger LOG = LoggerFactory.getLogger(ZookeeperProcessor.class);

    @Autowired
    private ZookeeperOperation zookeeperOperation;

    private TreeCacheListener configListener;

    @PostConstruct
    public void initialize() {
        String group = getGroup();
        String dataId = getDataId();
        String key = group + "-" + dataId;
        String description = getDescription();
        String configType = getConfigType();

        LOG.info("Get {} config from {} server, key={}", description, configType, key);

        try {
            String config = zookeeperOperation.getConfig(group, dataId);

            callbackConfig(config);
        } catch (Exception e) {
            LOG.info("Get {} config from {} server failed, key={}", description, configType, key, e);
        }

        LOG.info("Subscribe {} config from {} server, key={}", description, configType, key);

        try {
            configListener = zookeeperOperation.subscribeConfig(group, dataId, new ZookeeperSubscribeCallback() {
                @Override
                public void callback(String config) {
                    try {
                        callbackConfig(config);
                    } catch (Exception e) {
                        LOG.error("Callback {} config failed", description, e);
                    }
                }
            });
        } catch (Exception e) {
            LOG.error("Subscribe {} config from {} server failed, key={}", description, configType, key, e);
        }
    }

    @Override
    public void destroy() {
        if (configListener == null) {
            return;
        }

        String group = getGroup();
        String dataId = getDataId();
        String key = group + "-" + dataId;
        String description = getDescription();
        String configType = getConfigType();

        LOG.info("Unsubscribe {} config from {} server, key={}", description, configType, key);

        try {
            zookeeperOperation.unsubscribeConfig(group, dataId, configListener);
        } catch (Exception e) {
            LOG.error("Unsubscribe {} config from {} server failed, key={}", description, configType, key, e);
        }
    }

    public String getConfigType() {
        return ZookeeperConstant.ZOOKEEPER_TYPE;
    }

    public abstract String getGroup();

    public abstract String getDataId();

    public abstract String getDescription();

    public abstract void callbackConfig(String config);
}
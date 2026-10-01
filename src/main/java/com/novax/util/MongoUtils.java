package com.novax.util;

import com.mongodb.BasicDBObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import org.apache.log4j.Logger;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class MongoUtils {
    static Logger logger = Logger.getLogger(MongoUtils.class);

    /** 每个 env 复用同一个 MongoClient（内置连接池），避免每次调用 new */
    private static final ConcurrentHashMap<String, MongoClient> CLIENT_MAP = new ConcurrentHashMap<>();

    private static MongoClient getClient(String env) {
        return CLIENT_MAP.computeIfAbsent(env, e -> {
            String host = PropertyUtil.getMongoHost(e);
            String port = PropertyUtil.getMongoPort(e);
            String username = PropertyUtil.getMongoUser(e);
            String password = PropertyUtil.getMongoPassword(e);
            String connectionString = "mongodb://" + username + ":" + password + "@" + host + ":" + port;
            logger.info("MongoClient created for env: " + e);
            return MongoClients.create(connectionString);
        });
    }

    // private static MongoClient getClient(String env) {
    //     return CLIENT_MAP.computeIfAbsent(env, e -> {
    //         String host     = PropertyUtil.getMongoHost(e);
    //         String port     = PropertyUtil.getMongoPort(e);
    //         String username = PropertyUtil.getMongoUser(e);
    //         String password = PropertyUtil.getMongoPassword(e);
    //         String[] hosts  = host.split(",");
    //         StringBuilder sb = new StringBuilder();
    //         for (int i = 0; i < hosts.length; i++) {
    //             if (i > 0) sb.append(",");
    //             sb.append(hosts[i].trim()).append(":").append(port);
    //         }
    //         String connectionString = "mongodb://" + username + ":" + password + "@" + sb;
    //         logger.info("MongoClient created for env=" + e + " hosts=" + sb);
    //         return MongoClients.create(connectionString);
    //     });
    // }

    public static MongoCollection<Document> getMongoConnection(String database, String collection, String env) {
        return getClient(env).getDatabase(database).getCollection(collection);
    }

    public static List<String> getRows(MongoCollection<Document> collection, BasicDBObject query) {
        FindIterable<Document> result = collection.find(query);
        ArrayList<String> rows = new ArrayList<>();
        for (Document document : result) {
            rows.add(document.toJson());
        }
        return rows;
    }
}

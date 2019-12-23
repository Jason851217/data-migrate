package com.example.datamigrate;

import lombok.extern.slf4j.Slf4j;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class DataMigrateApplication {

    private static DataSourceProperties dataSourceProperties = DataSourceProperties.getInstance();

    public static void main(String[] args) {
        log.info("配置信息:{}", dataSourceProperties);
        startMigrate();
    }

    public static void startMigrate() {
        List<Map<String, Object>> listMap = loadDataFromSource();
        if (listMap != null && listMap.size() > 0) {
            log.info("》》》》》》》导入开始");
            importData2Target(listMap);
            log.info("》》》》》》》导入完成");
        }
    }

    /**
     * 从源数据库中加载相应的数据
     *
     * @return
     */
    private static final List<Map<String, Object>> loadDataFromSource() {
        //从源数据库读取数据
        List<Map<String, Object>> listMap = new ArrayList<Map<String, Object>>();
        Connection sourceConn = null;
        Statement stmt = null;
        ResultSet rs = null;
        try {
            Class.forName(dataSourceProperties.getSourceDriver());
            sourceConn = DriverManager.getConnection(dataSourceProperties.getSourceUrl(), dataSourceProperties.getSourceUsername(),
                    dataSourceProperties.getSourcePassword());
            String sql = "select EventLogKey,Value,Quality,QualityDetail from AnalogSnapshot";
            stmt = sourceConn.createStatement();
            rs = stmt.executeQuery(sql);
            while (rs.next()) {
                Map<String, Object> map = new HashMap<String, Object>();
                //将查询出的结果集中的数据分别用变量存放
                map.put("EventLogKey", rs.getInt("EventLogKey"));
                map.put("Value", rs.getFloat("Value"));
                map.put("Quality", rs.getInt("Quality"));
                map.put("QualityDetail", rs.getInt("QualityDetail"));
                listMap.add(map);
            }
            log.info("listMap:{}", listMap);
            log.info("lise size:{}", listMap.size());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (sourceConn != null) {
                try {
                    sourceConn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
        return listMap;
    }

    /**
     * 导入数据到目标数据库
     *
     * @param listMap
     */
    private final static void importData2Target(List<Map<String, Object>> listMap) {
        Connection targetConn = null;
        PreparedStatement preparedStatement = null;
        try {
            //导入数据到目标数据库
            Class.forName(dataSourceProperties.getTargetDriver());
            targetConn = DriverManager.getConnection(dataSourceProperties.getTargetUrl(), dataSourceProperties.getTargetUsername(),
                    dataSourceProperties.getTargetPassword());
            preparedStatement = targetConn.prepareStatement("insert into \"Test\"(\"id\",\"EventLogKey\",\"Value\",\"Quality\",\"QualityDetail\") values(?,?,?,?,?)");
            for (int i = 0; i < 300; i++) {
                Integer eventLogKey = (Integer) listMap.get(i).get("EventLogKey");
                Float value = (Float) listMap.get(i).get("Value");
                Integer quality = (Integer) listMap.get(i).get("Quality");
                Integer qualityDetail = (Integer) listMap.get(i).get("QualityDetail");
                String insertSql = "insert into \"Test\"(id,EventLogKey,Value,Quality,QualityDetail)"
                        + "values(" + i + ",'" + eventLogKey + "','" + value + "','" + quality + "','" + qualityDetail + "')";
                log.info("sql:{}", insertSql);
                preparedStatement.setInt(1, i);
                preparedStatement.setInt(2, eventLogKey);
                preparedStatement.setFloat(3, value);
                preparedStatement.setInt(4, quality);
                preparedStatement.setInt(5, qualityDetail);
                preparedStatement.addBatch();
                //每300条执行一次
                if ((i + 1) % 300 == 0) {
                    preparedStatement.executeBatch();
                    preparedStatement.clearBatch();
                }
            }
            //不足300条再执行一次
            preparedStatement.executeBatch();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (targetConn != null) {
                try {
                    targetConn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }

        }
    }


}

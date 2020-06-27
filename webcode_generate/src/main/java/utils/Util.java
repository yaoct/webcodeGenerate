package utils;

import java.io.*;
import java.net.URL;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Util {

    private static final String DRIVER = "com.mysql.cj.jdbc.Driver";
    private static final String URL = "jdbc:mysql://127.0.0.1:3306/blog?characterEncoding=UTF8&serverTimezone=UTC";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "root";

    private static final String SQL = "SELECT * FROM ";// 数据库操作

    static {
        try {
            Class.forName(DRIVER);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取数据库连接
     *
     * @return
     */
    public static Connection getConnection() {
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(URL, USERNAME, PASSWORD);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return conn;
    }

    /**
     * 关闭数据库连接
     *
     * @param conn
     */
    public static void closeConnection(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 获取数据库下的所有表名
     */
    public static List<String> getTableNames() {
        List<String> tableNames = new ArrayList<>();
        Connection conn = getConnection();
        ResultSet rs = null;
        try {
            //获取数据库的元数据
            DatabaseMetaData db = conn.getMetaData();
            //从元数据中获取到所有的表名
//            rs = db.getTables(null, "blog", null, new String[] { "TABLE" });
            rs = db.getTables(conn.getCatalog(), conn.getCatalog(), "%", new String[]{"TABLE"});
            System.out.println(conn.getCatalog());
            //            返回格式
//            TABLE_CAT字符串=>表目录（可能为null ）
//            TABLE_SCHEM字符串=>表格式（可能为null ）
//            TABLE_NAME字符串=>表名
//            TABLE_TYPE字符串=>表类型。 典型的类型是“TABLE”，“VIEW”，“SYSTEM TABLE”，“GLOBAL TEMPORARY”，“LOCAL TEMPORARY”，“ALIAS”，“SYNONYM”。
//            备注字符串=>对表的解释性评论
//            TYPE_CAT字符串=>类型目录（可能为null ）
//            TYPE_SCHEM字符串=>类型模式（可能是null ）
//            TYPE_NAME字符串=>类型名称（可能为null ）
//            SELF_REFERENCING_COL_NAME字符串=>类型表的指定“标识符”列的名称（可能为null ）
//            REF_GENERATION String =>指定如何创建SELF_REFERENCING_COL_NAME中的值。 值为“SYSTEM”，“USER”，“DERIVED”。 （可能是null ）
            while (rs.next()) {
                tableNames.add(rs.getString(3));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                rs.close();
                closeConnection(conn);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return tableNames;
    }

    /**
     * 获取表中所有字段名称
     *
     * @param tableName 表名
     * @return
     */
    public static List<String> getColumnNames(String tableName) {
        List<String> columnNames = new ArrayList<>();
        //与数据库的连接
        Connection conn = getConnection();
        ResultSet rs = null;
        try {
            DatabaseMetaData metaData = conn.getMetaData();
            rs = metaData.getColumns(conn.getCatalog(), conn.getCatalog(), "%", null);
            while (rs.next()) {
                System.out.println("TABLE_CAT:" + rs.getString("TABLE_CAT"));
                System.out.println("TABLE_SCHEM:" + rs.getString("TABLE_SCHEM"));
                System.out.println("TABLE_NAME:" + rs.getString("TABLE_NAME"));
                System.out.println("COLUMN_NAME:" + rs.getString("COLUMN_NAME"));
                System.out.println("DATA_TYPE:" + rs.getString("DATA_TYPE"));
                System.out.println("TYPE_NAME:" + rs.getString("TYPE_NAME"));
                System.out.println("COLUMN_SIZE:" + rs.getString("COLUMN_SIZE"));
                System.out.println("REMARKS:" + rs.getString("REMARKS"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                    closeConnection(conn);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
        return columnNames;
    }

    public static void main(String[] args) {
        List<String> tableNames = getTableNames();
        System.out.println("tableNames:" + tableNames);
        for (String tableName : tableNames) {
            String sqlType = getPrimaryKeyType(tableName);
            if (sqlType == null) continue;
            System.out.println("tableName:" + tableName + ",,,,,,,,,,,,,keyType:" + sqlType);
            String javaType = getType(sqlType);
            generate(tableName, javaType);
        }
//        File file = new File("");
//        System.out.println(file.getAbsolutePath());
//        System.out.println(System.getProperty("user.dir"));
//        System.out.println(Util.class.getResource("/"));
//        System.out.println(Util.class.getResource(""));
    }

    private static void generate(String tableName, String keyType) {
        tableName = tableName.substring(0, 1).toUpperCase() + tableName.substring(1);
        generateRepository(tableName, keyType);
        generateReService(tableName, keyType);
    }

    private static void generateReService(String tableName, String keyType) {
        HashMap<String, String> map = new HashMap<>();
        map.put("[entity]", tableName);
        map.put("[keyID]", keyType);
        map.put("[package]", "com.zhengyuan");
        String path = Util.class.getResource("/").toString();
        path = path.substring(path.indexOf("/") + 1);
        File inputFile = new File(path + "TemplateService");
        File dir = new File(path + "service");
        if (!dir.exists()) {
            dir.mkdir();
        }
        File outputFile = new File(dir + File.separator + tableName + "Service.java");
        BufferedReader br = null;
        BufferedWriter bw = null;
        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(inputFile), "UTF-8"));
            bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile), "UTF-8"));
            String line = null;
            while ((line = br.readLine()) != null) {
                for (String key : map.keySet()) {
                    line = line.replace(key, map.get(key));
                }
                bw.write(line + "\n");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                br.close();
                bw.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    private static void generateRepository(String tableName, String keyType) {
        HashMap<String, String> map = new HashMap<>();
        map.put("[entity]", tableName);
        map.put("[keyID]", keyType);
        map.put("[package]", "com.zhengyuan");
        String path = Util.class.getResource("/").toString();
        path = path.substring(path.indexOf("/") + 1);
        File inputFile = new File(path + "TemplateRepository");
        File dir = new File(path + "repository");
        if (!dir.exists()) {
            dir.mkdir();
        }
        File outputFile = new File(dir + File.separator + tableName + "Repository.java");
        BufferedReader br = null;
        BufferedWriter bw = null;
        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(inputFile), "UTF-8"));
            bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile), "UTF-8"));
            String line = null;
            while ((line = br.readLine()) != null) {
                for (String key : map.keySet()) {
                    line = line.replace(key, map.get(key));
                }
                bw.write(line + "\n");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                br.close();
                bw.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    static String getType(String type) {
        String resType = "String";
        if (type.equals("INT")) {
            resType = "Integer";
        } else if (type.equals("BIGINT")) {
            resType = "Long";
        } else if (type.equals("VARCHAR")) {
            resType = "String";
        }
        return resType;
    }

    //类型
    private static String getPrimaryKeyType(String tableName) {
        //与数据库的连接
        Connection conn = getConnection();
        ResultSet rs = null;
        try {
            DatabaseMetaData metaData = conn.getMetaData();
            rs = metaData.getPrimaryKeys(conn.getCatalog(), conn.getCatalog(), tableName);
            while (rs.next()) {
                String keyName = rs.getString("COLUMN_NAME");
                ResultSet rs2 = null;
                try {
                    rs2 = metaData.getColumns(conn.getCatalog(), conn.getCatalog(), tableName, null);
                    while (rs2.next()) {
                        if (rs2.getString("COLUMN_NAME").equals(keyName)) {
                            return rs2.getString("TYPE_NAME");
                        }
                    }
                } finally {
                    try {
                        rs2.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }

            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                    closeConnection(conn);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }
}
import org.junit.Test;

import java.sql.*;

public class MyBatisTest {

    @Test
    public void test01() throws Exception {
        String url = "jdbc:mysql://dev.allthinker.com:3306/renergy_dev?characterEncoding=UTF-8&useUnicode=true&useSSL=false&tinyInt1isBit=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai";
        String username = "renergy_dev";
        String password = "atred228";
        Class<?> clz = com.mysql.cj.jdbc.Driver.class;
        String sql = "SELECT id, project_id, bpm_node_id, approver, approval_result, approval_remark, approval_time, is_committed, oper_description, del_flag, create_by, create_time, update_by, update_time, sys_org_code, tenant_id FROM pv_project_bpm WHERE del_flag = '0' AND approval_result = 'rejected' AND project_id = 324 ORDER BY approval_time DESC LIMIT 1";
        try (Connection connection = DriverManager.getConnection(url, username, password)) {
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(sql);
            ResultSetMetaData metaData = resultSet.getMetaData();
            int columnCount = metaData.getColumnCount();

            while (resultSet.next()) {
                for (int i = 1; i <= columnCount ; i++) {
                    System.out.print(resultSet.getString(i));
                    System.out.print("\t");
                }
                System.out.println();
            }
            resultSet.close();
            statement.close();
        }

    }

}

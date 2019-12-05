package bistu.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;

import bistu.share.Detail;
import bistu.share.Overview;

public class DbUtil {

    private static final String URL = "jdbc:mariadb://[::]:3306/db_sticky_android";
    private static final String USER = "sticky_android";
    private static final String PASSWD = "android";
    private static final String LogName = "database util";

    public static final String TABLE_NAME = "sticky";
    public static final String COL_ID = "c_id";
    public static final String COL_MODIFY = "c_modify";
    public static final String COL_OVERVIEW = "c_overview";
    public static final String COL_FULL_TEXT = "c_full_text";

    private static DbUtil db;

    private Connection connection;
    private PreparedStatement s_selectList;
    private PreparedStatement s_selectSticky;
    private PreparedStatement s_updateSticky;
    private PreparedStatement s_removeSticky;
    private PreparedStatement s_insertSticky;
    private PreparedStatement s_lastInsertId;
    private Logger logger;

    private DbUtil() {
        try {
            this.logger = Logger.getLogger(LogName);
            this.connection = DriverManager.getConnection(URL, USER, PASSWD);
            logger.log(Level.INFO, "connected to database.");
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "connect to database failed!", e);
            System.exit(-1);
        }

        try {
            this.s_selectList = connection.prepareStatement(String.format("select %s, %s, %s from %s;", COL_ID, COL_MODIFY, COL_OVERVIEW, TABLE_NAME));
            this.s_selectSticky = connection.prepareStatement(String.format("select * from %s where %s=?;", TABLE_NAME, COL_ID));
            this.s_updateSticky = connection.prepareStatement(String.format("update %s set %s=?, %s=? where %s=?", TABLE_NAME, COL_MODIFY, COL_FULL_TEXT, COL_ID));
            this.s_removeSticky = connection.prepareStatement(String.format("delete from %s where %s=?", TABLE_NAME, COL_ID));
            this.s_insertSticky = connection.prepareStatement(String.format("insert into %s (%s) values (now());", TABLE_NAME, COL_MODIFY));
            this.s_lastInsertId = connection.prepareStatement("select last_insert_id()");
            logger.log(Level.INFO, "prepared statement.");
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "preparing statement error!", e);
            System.exit(-1);
        }
    }

    public static DbUtil getInstance() {
        if (db == null) {
            db = new DbUtil();
            return db;
        } else {
            return db;
        }
    }

    /**
     * get id, modify time, overview from table
     * @return return a List<Overview>, null if error occurred.
     */
    public List<Overview> getList() {
        ResultSet rs = null;
        try {
            rs = s_selectList.executeQuery();
            logger.log(Level.INFO, "selected full list from table.");
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "error occurred while selecting sticky full list!", e);
            return null;
        }

        ArrayList<Overview> result = new ArrayList<>();
        try {
            while (rs.next()) {
                Overview o = new Overview(
                    rs.getLong(COL_ID),
                    rs.getTimestamp(COL_MODIFY),
                    rs.getString(COL_OVERVIEW)
                );
                result.add(o);
            }
            logger.log(Level.INFO, String.format("retrieved %d stickies from table.", result.size()));
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "error occurred while retrieving data from resultSet!", e);
            return null;
        }
        return result;
    }

    /**
     * get detail of a sticky.
     * @param id the required sticky's id
     * @return Detail of sticky, null if error occurred.
     */
    public Detail getDetail(long id) {
        ResultSet rs = null;
        try {
            s_selectSticky.setLong(1, id);
            rs = s_selectSticky.executeQuery();
            s_selectSticky.clearParameters();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "error occurred while querying detail.", e);
            return null;
        }

        Detail d = null;
        try {
            d = new Detail(
                rs.getLong(COL_ID),
                rs.getTimestamp(COL_MODIFY),
                rs.getString(COL_FULL_TEXT)
            );
            logger.log(Level.INFO, String.format("detail got: %s", d.toString()));
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "error occurred while retrieving data from resultSet.", e);
            return null;
        }
        return d;
    }

    /**
     * update a sticky.
     * @param sticky the new sticky, should has the same id of previous sticky.
     * @return true if update succeed, false if update fail or error occurred.
     */
    public boolean updateSticky(Detail sticky) {
        boolean result = false;
        try {
            s_updateSticky.setTimestamp(1, sticky.getModifyTime());
            s_updateSticky.setString(2, sticky.getFullText());
            s_updateSticky.setLong(3, sticky.getId());
            result = s_updateSticky.executeUpdate() > 0;
            s_updateSticky.clearParameters();
            logger.log(Level.INFO, "sticky updated.");
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "error occurred while updating sticky.", e);
        }
        return result;
    }

    /**
     * @param id the id of sticky to be removed
     * @return true if remove succeed, false if fail or error occurred.
     */
    public boolean removeSticky(long id) {
        boolean result = false;
        try {
            s_removeSticky.setLong(1, id);
            result = s_removeSticky.executeUpdate() > 0;
            logger.log(Level.INFO, result ? "remove sticky succeed.": "removed nothing.");
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "error occurred while removing sticky.", e);
        }
        return result;
    }

    /**
     * will create a blank sticky in database. the modify time of sticky depends on when
     * the sql be run on server.
     * @return sticky's id if succeed, -1 if create fail.
     */
    public long insertSticky() {
        long id = -1;
        try {
            if (s_insertSticky.executeUpdate() > 0) {
                id = s_lastInsertId.executeQuery().getLong(1);
                logger.log(Level.INFO, String.format("inserted id=%d.", id));
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "error occurred while inserting or getting id.");
        }
        return id;
    }

}
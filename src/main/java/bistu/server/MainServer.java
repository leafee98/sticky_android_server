package bistu.server;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.net.ServerSocket;
import java.net.Socket;

import bistu.db.DbUtil;

public class MainServer {

    private static final String LOGGER_NAME = "sticky_android server";
    private static final int SERVE_PORT = 1234;

    private Logger logger;
    private ServerSocket ssocket;

    private Throwable loadJdbcDriver() {
        Throwable t = null;
        try {
            Class.forName("org.mariadb.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            t = e;
        }
        return t;
    }

    private Throwable initServerSocket() {
        Throwable t = null;
        try {
            this.ssocket = new ServerSocket(SERVE_PORT);
            logger.log(Level.INFO, String.format("Listening port: %d", SERVE_PORT));
        } catch (IOException e) {
            t = e;
        }
        return t;
    }

    private void serve() {
        while (true) {
            try {
                logger.log(Level.INFO, "waitting for user connecting...");
                Socket socket = this.ssocket.accept();
                new Thread(new Serve(socket)).start();
            } catch (IOException e) {
                logger.log(Level.SEVERE, "there is a connection failed, relistening the same port.", e);
            }
        }
    }

    private void initServer() {
        this.logger = Logger.getLogger(LOGGER_NAME);
        logger.log(Level.INFO, "inited the logger.");

        Throwable loadJdbcDriverThrow = this.loadJdbcDriver();
        if (loadJdbcDriverThrow == null) {
            logger.log(Level.INFO, "load the jdbc driver.");
        } else {
            logger.log(Level.SEVERE, "load jdbc drive error!", loadJdbcDriverThrow);
            System.exit(-1);
        }

        logger.log(Level.INFO, "initializing DbUtil...");
        DbUtil.getInstance();
        logger.log(Level.INFO, "initialize DbUtil succeed.");

        Throwable initServerSocketThrow = this.initServerSocket();
        if (initServerSocketThrow == null) {
            logger.log(Level.INFO, "inited the server socket.");
        } else {
            logger.log(Level.SEVERE, "init server socket error!", initServerSocketThrow);
            System.exit(-1);
        }

        logger.log(Level.INFO, "server start successfully.");
        logger.log(Level.INFO, "start listen from user.");
        this.serve();

    }

    public static void main(String[] args) {
        MainServer s = new MainServer();
        s.initServer();
    }

}
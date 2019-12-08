package bistu.server;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.List;

import bistu.share.Detail;
import bistu.share.Overview;
import bistu.share.Instruction;
import bistu.db.DbUtil;

public class Serve implements Runnable {

    private Socket socket;
    private DbUtil db;
    private String logName;
    private Logger logger;

    private ObjectInputStream objectIn;
    private ObjectOutputStream objectOut;

    private boolean fine = true;

    Serve(Socket socket) {
        this.socket = socket;
        int port = socket.getPort();
        String addr = socket.getInetAddress().getHostAddress();
        this.logName = String.format("Serve for %s:%d", addr, port);
        this.logger = Logger.getLogger(this.logName);
        this.db = DbUtil.getInstance();
    }

    private void serve_SERVE_END() throws IOException {
        objectIn.close();
        objectOut.close();
        this.fine = false;
        logger.log(Level.INFO, "received SERVE_END sign, finish service.");
    }

    private void serve_GET_LIST() throws IOException {
        List<Overview> stickies = db.getList();
        if (stickies != null) {
            objectOut.writeInt(Instruction.FINE_CODE);
            objectOut.writeObject(stickies);
        } else {
            objectOut.writeInt(Instruction.ERR_CODE);
        }
        objectOut.flush();
    }

    private void serve_GET_DETAIL() throws IOException {
        long id = objectIn.readLong();
        Detail detail = db.getDetail(id);
        if (detail != null) {
            objectOut.writeInt(Instruction.FINE_CODE);
            objectOut.writeObject(detail);
        } else {
            objectOut.writeInt(Instruction.ERR_CODE);
        }
        objectOut.flush();
    }

    private void serve_ADD_STICKY() throws IOException {
        long id = db.insertSticky();
        if (id > 0) {
            objectOut.writeInt(Instruction.FINE_CODE);
            objectOut.writeLong(id);
        } else {
            objectOut.writeInt(Instruction.ERR_CODE);
        }
        objectOut.flush();
    }

    private void serve_UPDATE_STICKY() throws IOException {
        try {
            Detail d = (Detail)objectIn.readObject();
            if (db.updateSticky(d)) {
                objectOut.writeInt(Instruction.FINE_CODE);
            } else {
                objectOut.writeInt(Instruction.ERR_CODE);
            }
            objectOut.flush();
            logger.log(Level.INFO, String.format("update: %s", d.toString()));
        } catch (ClassNotFoundException e) {
            logger.log(Level.SEVERE, "the class Detail not found while reading updated sticky.", e);
        }
    }
    
    private void serve_REMOVE_STICKY() throws IOException {
        long id = objectIn.readLong();
        if (db.removeSticky(id)) {
            objectOut.writeInt(Instruction.FINE_CODE);
        } else {
            objectOut.writeInt(Instruction.ERR_CODE);
        }
        objectOut.flush();
    }

    private void serve() {
        while (this.fine) {
            try {
                switch (objectIn.readInt()) {
                    case Instruction.SERVE_END:
                        this.serve_SERVE_END();
                        break;

                    case Instruction.GET_LIST:
                        this.serve_GET_LIST();
                        break;

                    case Instruction.GET_DETAIL:
                        this.serve_GET_DETAIL();
                        break;

                    case Instruction.ADD_STICKY:
                        this.serve_ADD_STICKY();
                        break;

                    case Instruction.UPDATE_STICKY:
                        this.serve_UPDATE_STICKY();
                        break;

                    case Instruction.REMOVE_STICKY:
                        this.serve_REMOVE_STICKY();
                        break;
                }
            } catch (IOException e) {
                logger.log(Level.SEVERE, "error while reading/writing socket. shutting this serve.", e);
                this.fine = false;
            }
        }
    }

    @Override
    public void run() {
        logger.log(Level.INFO, "a user connected.");

        try {
            this.objectIn = new ObjectInputStream(socket.getInputStream());
            this.objectOut = new ObjectOutputStream(socket.getOutputStream());
            logger.log(Level.INFO, "inited the object stream.");
        } catch (IOException e) {
            logger.log(Level.SEVERE, "error occurred while initializing object stream.", e);
            this.fine = false;
        }

        this.serve();
    }

}
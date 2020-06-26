package org;


import org.apache.log4j.Logger;
import org.niss.server.TcpServer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;



/**
 * @author Ni187
 */
public class ServerApplication{

    public static void main(String[] args) throws Exception{
        Logger logger = Logger.getLogger(ServerApplication.class);
        String welcome = "<------------------------------------>\n" +
                "|===========START  SERVER===========|\n" +
                "<------------------------------------>";
        System.out.println(welcome);
        ApplicationContext appContext = new ClassPathXmlApplicationContext("application-config.xml");
        TcpServer tcpServer = appContext.getBean("tcpServer", TcpServer.class);
        logger.info("start server success.");
        tcpServer.start();
    }
}





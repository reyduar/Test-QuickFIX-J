package ar.com.aduarte.ejemplofix;

import java.util.Date;

import org.junit.Assert;
import org.junit.Test;


import quickfix.Application;
import quickfix.ConfigError;
import quickfix.DefaultMessageFactory;
import quickfix.MemoryStoreFactory;
import quickfix.ScreenLogFactory;
import quickfix.SessionSettings;
import quickfix.SocketAcceptor;
import quickfix.SocketInitiator;
import quickfix.field.ClOrdID;
import quickfix.field.OrdType;
import quickfix.field.OrderQty;
import quickfix.field.Side;
import quickfix.field.TransactTime;
import quickfix.fix50.NewOrderSingle;




public class RunFixTest {

	@Test
	  public  void test() throws Exception {
	    iniciarServidor();
	    String password = "password";
	    String usuario = "usuario";
	    NewOrderSingle newOrder = new NewOrderSingle(new ClOrdID("12345"), new Side(Side.BUY), 
	        new TransactTime(new Date()), new OrdType(OrdType.MARKET));
	    newOrder.set(new OrderQty(1000));
	    ExampleClientApplication application = new ExampleClientApplication(newOrder, usuario, password);
	    iniciarCliente(application);
	    Thread.sleep(5000L); // 5 segundos
	    Assert.assertTrue(application.estaLogueado());
	    Assert.assertTrue(application.seEjecutoOrdenCorrectamente());
	  }
	
	  private  void iniciarCliente(Application application) throws ConfigError {
	    SessionSettings settings = new SessionSettings(RunFixTest.class.getResourceAsStream("/cliente.cfg"));
	    SocketInitiator socketInitiator = new SocketInitiator(application, new MemoryStoreFactory(), settings,
	            new ScreenLogFactory(), new DefaultMessageFactory());
	    socketInitiator.start();
	  }

	  private  void iniciarServidor() throws ConfigError {
	    SessionSettings settings = new SessionSettings(RunFixTest.class.getResourceAsStream("/servidor.cfg"));
	    ExampleServerApplication application = new ExampleServerApplication();
	    SocketAcceptor acceptor = new SocketAcceptor(application, new MemoryStoreFactory(), settings,
	            new ScreenLogFactory(), new DefaultMessageFactory());
	    acceptor.start();
	  }
}

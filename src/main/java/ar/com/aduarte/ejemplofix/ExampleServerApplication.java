package ar.com.aduarte.ejemplofix;

import quickfix.ApplicationAdapter;
import quickfix.FieldNotFound;
import quickfix.IncorrectTagValue;
import quickfix.Message;
import quickfix.RejectLogon;
import quickfix.Session;
import quickfix.SessionID;
import quickfix.SessionNotFound;
import quickfix.UnsupportedMessageType;
import quickfix.field.ClOrdID;
import quickfix.field.CumQty;
import quickfix.field.ExecID;
import quickfix.field.ExecType;
import quickfix.field.LeavesQty;
import quickfix.field.OrdStatus;
import quickfix.field.OrderID;
import quickfix.fix50.ExecutionReport;
import quickfix.fix50.NewOrderSingle;
import quickfix.fixt11.Logon;

public class ExampleServerApplication extends ApplicationAdapter {
	
	@Override
	  public  void fromAdmin(Message message, SessionID sessionId) throws FieldNotFound, RejectLogon {
	    if (message instanceof Logon) {
	      if (!usuarioYPasswordCorrectos((Logon) message)) {
	        throw new RejectLogon();
	      }
	    }
	  }

	  private  boolean usuarioYPasswordCorrectos(Logon logon) throws FieldNotFound {
	    return logon.getUsername().getValue().equals("usuario") 
	        && logon.getPassword().getValue().equals("password");
	  }

	  @Override
	  public  void fromApp(Message message, SessionID sessionId) throws FieldNotFound, IncorrectTagValue,
	          UnsupportedMessageType {
	    if (message instanceof NewOrderSingle) {
	      NewOrderSingle newOrderSingle = ((NewOrderSingle) message);
	      ExecutionReport executionReport = new ExecutionReport();
	      String clOrdID = newOrderSingle.getClOrdID().getValue();
	      executionReport.set(new ClOrdID(clOrdID));
	      executionReport.set(new ExecID("98765"));
	      executionReport.set(new OrderID("99999"));
	      executionReport.set(newOrderSingle.getSide());
	      executionReport.set(new OrdStatus(OrdStatus.NEW));
	      executionReport.set(new CumQty(0));
	      executionReport.set(new ExecType(ExecType.NEW));
	      executionReport.set(new LeavesQty(newOrderSingle.getOrderQty().getValue()));
	      try {
	        Session.sendToTarget(executionReport, sessionId);
	      } catch (SessionNotFound e) {
	        throw new RuntimeException(e);
	      }
	    }
	  }

}

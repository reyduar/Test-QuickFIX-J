# Ejemplos de Cliente y Servidor en QuickFIX/J

Vamos a hacer un cliente de ejemplo en QuickFIX/J. Este cliente se conectará a un servidor, y apenas se conecte enviará una orden de compra de un instrumento ficticio, el servidor responderá con otro mensaje, y cuando el cliente reciba la respuesta de la compra, seteará una variable, y observaremos la respuesta de esa variable. Con este ejemplo, si bien irreal, veremos como se utiliza QuickFIX/J como cliente y como servidor.

## Introducción

El protocolo **FIX ** (Financial Information Exchange Protocol) es un protocolo de mensajes para el comercio de instrumentos financieros. FIX se utiliza ampliamente para la comunicación automática entre los participantes del intercambio de instrumentos, y especifica como son los mensajes para crear ordenes de compra y venta y consultar cotizaciones de instrumento, entre otros. Este protocolo es el que hay que utilizar para comunicarnos con practicamente todos los mercados financieros de manera electrónica.

En este articulo, veremos los aspectos básicos del protocolo FIX, como utilizarlo mediante la librería de java QuickFIX/J mediante un ejemplo como cliente y servidor, y algunos aspectos poco obvios que hay que tener en cuenta al utilizarlo.

## Cómo es el formato FIX

El formato FIX es un formato textual, donde cada mensaje es una única linea de la forma:

NUMERO_DE_CAMPO=CONTENIDO[SEPARADOR]NUMERO_DE_CAMPO=CONTENIDO[SEPARADOR]....

El separador es el caracter unicode representado en Java y otros como "\u0001". Los campos se identifican por su número, y el tipo de mensaje es un campo más dentro del mensaje (el campo 35), y el último campo es el campo 10 que es el checksum. El orden de los campos importa, y está especificado para cada mensaje FIX. Las estructuras repetitivas en FIX se llaman Repeating Group y se definen simplemente definiendo un campo cabecera del Repeating Group que indica la cantidad de elementos contenidos, y todos los elementos se diferencian entre sí detectando el primer campo de cada elemento, esto funciona precisamente porque importa el orden de los campos.


## QuickFIX/J

FIX es un protocolo abierto y gratuito; es solo la especificación de los mensajes y de los campos de los mismos, y existen varias implementaciones de este protocolo. QuickFIX/J es una implementación gratuita y open source (con una licencia propia similar a BSD/MIT) para Java (es un port de QuickFIX, que está escrita en C++), que es muy fácil de utilizar para comunicarnos con cualquier participante, ya sea como cliente o servidor.

Incluida esta librería en nuestro proyecto, tenemos que implementar nuestro cliente, y para esto QuickFIX/J nos pide que implementemos la interfaz `quickfix.Application`. QuickFIX/J es un framework que trabaja con el concepto de Inversión de Control: en lugar de invocar los métodos de QuickFIX/J para loguearnos, ir recibiendo los mensajes, etc; nosotros tenemos que darle un objeto `Application` y QuickFIX/J invoca a los métodos de ese objeto con los eventos que vayan ocurriendo, en sus propios threads.

La interfaz `Application` define métodos para cada evento que ocurra durante la comunicación, por ejemplo `onLogon`, `onLogout`, `fromApp` (se recibió un mensaje de negocio), `fromAdmin` (se recibio un mensaje relativo a la sesión, como por ejemplo rechazo de un mensaje mal formado), entre otros. Nosotros, en vez de implementar `Application` directamente, vamos a heredar de `Application`Adapter que es una clase abstracta que tiene definiciones vacías para todos estos métodos por default. Es la misma interfaz tanto para Cliente como para Servidor.

Esta clase en su constructor recibirá el mensaje `NewOrderSingle` de creación de una orden, que el cliente deberá enviar al loguearse. En el método `onLogon`, el cual QuickFIX/J invoca luego de un logueo exitoso, realiza el envío del mensaje con el método estático `Session.sendToTarget`.

En el método `fromApp`, se reciben los mensajes de negocio, y lo que buscamos es un mensaje de `ExecutionReport`, que es el mensaje de FIX con el que se reportan los cambios de estado de una orden (cuando es creada, cuando es cancelada, a medida que se va ejecutando, cuando es completada, etc).

Si el campo `ClOrdID` (el cual representa el id de orden del cliente) del `ExecutionReport` coincide con el que mandamos en el `NewOrderSingle`, seteamos el flag de `seEjecutoOrdenCorrectamente`.

Finalmente en el método toAdmin, que se invoca inmediatamente antes de enviar un mensaje "administrativo" (es decir, un mensaje que no es de negocio), interceptamos el mensaje Logon (que se crea automáticamente) y le asignamos usuario y password (sí, esa es la forma estándar de definir el usuario y password de la conexión).


Este es el `Application` del servidor de ejemplo. En el método `fromAdmin` (donde se reciben los mensajes administrativos), validamos el Login, si no coincide el usuario y password, lanzamos la excepcion RejectLogin.

Nota: en este esquema sencillo, el password viaja como texto plano: Las conexiones de FIX tienen que segurizarse por medio de algún esquema como SSL, utilizar una VPN, o similar.

En el metodo `fromApp` recibimos el `NewOrderSingle` y creamos un mensaje de `ExecutionReport` y le asignamos varios campos (en este ejemplo, solo los obligatorios). En el campo `ClOrdID`, copiamos el dato recibido por el `NewOrderSingle`, para que el cliente pueda detectar a que `ExecutionReport` corresponde. Y finalmente enviamos el mensaje.

n este test inicializamos el cliente y el servidor, y vemos que efectivamente el cliente se loguea contra el servidor, envia la orden, y recibe la respuesta. Notese que el cliente y el servidor se construyen de maneras distintas pero similares: el primero con un SocketIniciator y el segundo con un SocketAcceptor. En el constructor de cada uno de ellos se pasan objetos que permiten parametrizar el comportamiento del motor de QuickFIX/J, definiendo el esquema de logging, como se guardan los mensajes, y cual es el Factory de los mensajes (que depende de la versión del protocolo de FIX, pero DefaultMessageFactory sirve para cualquier versión).

Hay que tener cuidado al hacer tests integrales con QuickFIX/J, ya que como el `Application` corre en un thread aparte, instanciado por QuickFIX/J, perdemos el control de la ejecución y necesitamos algún tipo de espera o sincronismo para asegurarnos de que cumplieron las acciones deseadas (en este caso, que se loguee el cliente contra el servidor y se envien entre ellos los mensajes `NewOrderSingle` y `ExecutionReport`). Por esto hay en el test un Thread.sleep con 5 segundos, si este fuera un test real, lo correcto sería utilizar polling con timeout, no Thread.sleep ya que es un método obviamente lento.

+ Ejemplo de Iniciador (cliente.cfg)

```
[default]
ConnectionType=initiator
StartTime=00:00:00
EndTime=00:00:00
HeartBtInt=300

[session]
BeginString=FIXT.1.1
DefaultApplVerID=FIX.5.0
SocketConnectHost=localhost
SocketConnectPort=9876
SenderCompID=CLIENTE
TargetCompID=SERVIDOR
```

+ Ejemplo Acceptor (servidor.cfg)

```
[default]
ConnectionType=acceptor

StartTime=00:00:00
EndTime=00:00:00

HeartBtInt=300
RejectInvalidMessage=N

[session]
BeginString=FIXT.1.1
DefaultApplVerID=FIX.5.0
SocketAcceptPort=9876
SenderCompID=SERVIDOR
TargetCompID=CLIENTE
```

ConnectionType identifica el tipo de conexión, es decir, si es servidor o cliente. StartTime y EndTime indican el inicio y fin de la sesión y está relacionado al tiempo de apertura y cierre de los mercados. `BeginString` indica la versión del protocolo de transporte de FIX, y DefaultApplVerID indica la versión del protocolo (las versiones anterior a la 5, no tienen un protocolo de transporte especifico diferenciado de la versión de FIX en sí, y se definen únicamente por el `BeginString` indicando el protocolo de FIX).

SenderCompID y `TargetCompID` son identificadores de los interlocutores de la sesión, y están invertidos en el Cliente y el Servidor. En FIX, la sesión es algo que se considera que va más allá de la conexión dada en un momento, si la conexión se cae y después se vuelve a conectar, la sesión sigue siendo la misma, y de hecho se reenvían los mensajes pendientes al momento de caerse la sesión (en QuickFIX/J para que esto funcione hay que instanciar el `SocketAcceptor/Initiator` con el `FileStoreFactory`, no el MemoryStoreFactory, el cual guarda los mensajes en un archivo para poder reenviarlos). Justamente porque la sesión es algo global, el `SessionID` de QuickFIX/J está definido básicamente por tres valores: SenderCompID, `TargetCompID` y `BeginString`.




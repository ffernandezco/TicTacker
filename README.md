# TicTacker
**Francisco Fernández Condado**

Desarrollo Avanzado de Software / Grado en Ingeniería Informática de Gestión y Sistemas de Información

_[UPV-EHU](https://www.ehu.eus) / Curso 2024-25_

**TicTacker** es una aplicación móvil para Android basada en Java + Gradle que permite ayudar a empresas y a autónomos a seguir un registro de su jornada laboral. Para ello, permite realizar un seguimiento completo de las horas y minutos trabajados cada día, permitiendo además configurar una jornada laboral con sus correspondientes horas y días laborales a fin de conocer las horas extraordinarias realizadas cada día y recibir notificaciones en caso de sobrepasar la jornada descrita. Permite a los empleados registrar su perfil para obtener una experiencia más personalizada y se sincroniza con un servidor PHP para almacenar toda la información en una base de datos MySQL remota. Es además posible personalizar su logotipo y el idioma para adaptarse a las necesidades de la empresa, así como generar ficheros CSV con los fichajes registrados que posteriormente puede importarse si así se desea o configurar recordatorios para fichar a tiempo.

En la [documentación del proyecto](doc/handout.pdf) se ofrece más información sobre cómo utilizarla y las funcionalidades disponibles.

> [!WARNING]  
> Esta aplicación realiza peticiones al servidor *ec2-51-44-167-78.eu-west-3.compute.amazonaws.com* cedido como parte de la asignatura. Es importante asegurarse de que se pueden realizar peticiones HTTP al mismo. Puede replicarse en otro escenario modificando los valores de conexión y usando los ficheros disponibles en el directorio *server*.
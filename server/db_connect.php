<?php
$DB_SERVER = "ec2-51-44-167-78.eu-west-3.compute.amazonaws.com";
$DB_USER = ""; # Modificar
$DB_PASS = ""; # Modificar
$DB_DATABASE = "";

# Se establece la conexión:
$con = mysqli_connect($DB_SERVER, $DB_USER, $DB_PASS, $DB_DATABASE);

#Comprobamos conexión
if (mysqli_connect_errno()) {
    echo 'Error de conexion: ' . mysqli_connect_error();
    exit();
}
?>
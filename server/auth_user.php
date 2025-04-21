<?php
require_once 'db_connect.php'; # Conexión MySQL

header('Content-Type: application/json');

# Autenticar usuario verificando que su contraseña es correcta

$data = json_decode(file_get_contents('php://input'), true);
$username = $data['username'];
$password = $data['password'];

$response = ["success" => false, "error" => ""];

$query = "SELECT * FROM users WHERE username = ?";
$stmt = $con->prepare($query);
$stmt->bind_param("s", $username);
$stmt->execute();
$result = $stmt->get_result();

if ($result->num_rows > 0) {
    $user = $result->fetch_assoc();
    if (password_verify($password, $user['password'])) {
        $response["success"] = true;
    } else {
        $response["error"] = "Usuario o contraseña incorrectos";
    }
} else {
    $response["error"] = "Usuario no encontrado en la BD";
}

echo json_encode($response);
$stmt->close();
$con->close();
?>
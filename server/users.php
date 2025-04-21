<?php
require_once 'db_connect.php'; # Conexión MySQL

header('Content-Type: application/json');

$data = json_decode(file_get_contents('php://input'), true);
$action = $data['action'];

$response = ["success" => false, "data" => []];

# Operaciones sobre la tabla de usuarios

if ($action == 'check_user') {
    # Comprobar si existe un usuario
    $query = "SELECT id FROM users WHERE username = ?";
    $stmt = $con->prepare($query);
    $stmt->bind_param("s", $data['username']);
    $stmt->execute();
    $result = $stmt->get_result();

    $response["data"]["exists"] = $result->num_rows > 0;
    $response["success"] = true;
    $stmt->close();
} elseif ($action == 'add_user') {
    # Registrar nuevo usuario
    $query = "INSERT INTO users (username, password) VALUES (?, ?)";
    $stmt = $con->prepare($query);
    $password = password_hash($data['password'], PASSWORD_DEFAULT); # Encriptar contraseña
    $stmt->bind_param("ss", $data['username'], $password);
    $response["success"] = $stmt->execute();
    $stmt->close();
} elseif ($action == 'update_password') {
    # Actualizar contraseña de un usuario
    $query = "UPDATE users SET password = ? WHERE username = ?";
    $stmt = $con->prepare($query);
    $password = password_hash($data['password'], PASSWORD_DEFAULT); # Encriptar contraseña
    $stmt->bind_param("ss", $password, $data['username']);
    $response["success"] = $stmt->execute();
    $stmt->close();
}

echo json_encode($response);
$con->close();
?>
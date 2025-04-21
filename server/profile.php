<?php
require_once 'db_connect.php'; # Conexión MySQL

header('Content-Type: application/json');

$data = json_decode(file_get_contents('php://input'), true);
$action = $data['action'];

$response = ["success" => false, "data" => []];

# Operaciones sobre la tabla de perfiles

if ($action == 'get_profile') {
    # Obtener perfil
    $query = "SELECT name, surname, birthdate, email, profile_photo FROM user_profiles WHERE username = ?";
    $stmt = $con->prepare($query);
    $stmt->bind_param("s", $data['username']);
    $stmt->execute();
    $result = $stmt->get_result();

    if ($row = $result->fetch_assoc()) {
        $response["data"] = $row;
        $response["success"] = true;
    }
    $stmt->close();
} elseif ($action == 'update_profile') {
    # Actualizar perfil
    // Comprobar si el perfil existe
    $checkQuery = "SELECT id FROM user_profiles WHERE username = ?";
    $checkStmt = $con->prepare($checkQuery);
    $checkStmt->bind_param("s", $data['username']);
    $checkStmt->execute();
    $checkResult = $checkStmt->get_result();
    $checkStmt->close();

    if ($checkResult->num_rows > 0) {
        // Actualizar el perfil existente
        $query = "UPDATE user_profiles SET name = ?, surname = ?, birthdate = ?, email = ?, profile_photo = ? WHERE username = ?";
        $stmt = $con->prepare($query);
        $stmt->bind_param("ssssss",
            $data['name'],
            $data['surname'],
            $data['birthdate'],
            $data['email'],
            $data['profile_photo'],
            $data['username']
        );
    } else {
        // Crear nuevo perfil si no existe
        $query = "INSERT INTO user_profiles (username, name, surname, birthdate, email, profile_photo) VALUES (?, ?, ?, ?, ?, ?)";
        $stmt = $con->prepare($query);
        $stmt->bind_param("ssssss",
            $data['username'],
            $data['name'],
            $data['surname'],
            $data['birthdate'],
            $data['email'],
            $data['profile_photo']
        );
    }

    $response["success"] = $stmt->execute();
    $stmt->close();
}

echo json_encode($response);
$con->close();
?>
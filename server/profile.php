<?php
require_once 'db_connect.php';

header('Content-Type: application/json');

$data = json_decode(file_get_contents('php://input'), true);
$action = $data['action'];

$response = ["success" => false, "data" => []];

if ($action == 'get_profile') {
    $query = "SELECT name, surname, birthdate, email FROM user_profiles WHERE username = ?";
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
    // Comprobar si existe el perfil
    $checkQuery = "SELECT id FROM user_profiles WHERE username = ?";
    $checkStmt = $con->prepare($checkQuery);
    $checkStmt->bind_param("s", $data['username']);
    $checkStmt->execute();
    $checkResult = $checkStmt->get_result();
    $checkStmt->close();

    if ($checkResult->num_rows > 0) {
        // Actualizar perfil existente
        $query = "UPDATE user_profiles SET name = ?, surname = ?, birthdate = ?, email = ? WHERE username = ?";
        $stmt = $con->prepare($query);
        $stmt->bind_param("sssss", $data['name'], $data['surname'], $data['birthdate'], $data['email'], $data['username']);
    } else {
        // Crear nuevo perfil si no existe
        $query = "INSERT INTO user_profiles (username, name, surname, birthdate, email) VALUES (?, ?, ?, ?, ?)";
        $stmt = $con->prepare($query);
        $stmt->bind_param("sssss", $data['username'], $data['name'], $data['surname'], $data['birthdate'], $data['email']);
    }

    $response["success"] = $stmt->execute();
    $stmt->close();
}

echo json_encode($response);
$con->close();
?>
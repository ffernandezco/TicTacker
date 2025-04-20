<?php
require_once 'db_connect.php';

header('Content-Type: application/json');

$data = json_decode(file_get_contents('php://input'), true);
$action = $data['action'];

$response = ["success" => false, "data" => []];

if ($action == 'update_token') {
    // Comprobar si el token existe en el servidor
    $query = "SELECT id FROM fcm_tokens WHERE username = ?";
    $stmt = $con->prepare($query);
    $stmt->bind_param("s", $data['username']);
    $stmt->execute();
    $result = $stmt->get_result();
    $stmt->close();

    if ($result->num_rows > 0) {
        // Actualizar token existente
        $query = "UPDATE fcm_tokens SET token = ?, updated_at = NOW() WHERE username = ?";
        $stmt = $con->prepare($query);
        $stmt->bind_param("ss", $data['token'], $data['username']);
    } else {
        // Añadir nuevo token
        $query = "INSERT INTO fcm_tokens (username, token, created_at, updated_at) VALUES (?, ?, NOW(), NOW())";
        $stmt = $con->prepare($query);
        $stmt->bind_param("ss", $data['username'], $data['token']);
    }

    $response["success"] = $stmt->execute();
    $stmt->close();
} elseif ($action == 'get_all_tokens') {
    $query = "SELECT token FROM fcm_tokens";
    $result = $con->query($query);

    $tokens = [];
    while ($row = $result->fetch_assoc()) {
        $tokens[] = $row['token'];
    }

    $response["data"]["tokens"] = $tokens;
    $response["success"] = true;
}

echo json_encode($response);
$con->close();
?>
<?php
require_once 'db_connect.php';

header('Content-Type: application/json');

$data = json_decode(file_get_contents('php://input'), true);
$action = $data['action'];

$response = ["success" => false, "data" => []];

if ($action == 'save') {
    $query = "SELECT id FROM settings LIMIT 1";
    $result = $con->query($query);

    if ($result->num_rows > 0) {
        $query = "UPDATE settings SET weekly_hours = ?, working_days = ? WHERE id = 1";
    } else {
        $query = "INSERT INTO settings (id, weekly_hours, working_days) VALUES (1, ?, ?)";
    }

    $stmt = $con->prepare($query);
    $stmt->bind_param("di", $data['weekly_hours'], $data['working_days']);
    $response["success"] = $stmt->execute();
    $stmt->close();
} elseif ($action == 'get') {
    $query = "SELECT weekly_hours, working_days FROM settings LIMIT 1";
    $result = $con->query($query);

    if ($result->num_rows > 0) {
        $row = $result->fetch_assoc();
        $response["data"] = [
            "WeeklyHours" => $row['weekly_hours'],
            "WorkingDays" => $row['working_days']
        ];
    } else {
        $response["data"] = [
            "WeeklyHours" => 40.0,
            "WorkingDays" => 5
        ];
    }
    $response["success"] = true;
}

echo json_encode($response);
$con->close();
?>
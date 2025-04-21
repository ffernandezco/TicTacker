<?php

# Funciones JWT de PHP compiladas necesarias para Firebase y FCM
# https://github.com/firebase/php-jwt

function base64url_encode($data) {
    return rtrim(strtr(base64_encode($data), '+/', '-_'), '=');
}

function create_jwt($client_email, $private_key) {
    $header = [
        "alg" => "RS256",
        "typ" => "JWT"
    ];

    $now = time();
    $claims = [
        "iss" => $client_email,
        "scope" => "https://www.googleapis.com/auth/firebase.messaging",
        "aud" => "https://oauth2.googleapis.com/token",
        "iat" => $now,
        "exp" => $now + 3600
    ];

    $jwt_header = base64url_encode(json_encode($header));
    $jwt_claims = base64url_encode(json_encode($claims));
    $signature_input = $jwt_header . "." . $jwt_claims;

    openssl_sign($signature_input, $signature, $private_key, "sha256WithRSAEncryption");
    $jwt_signature = base64url_encode($signature);

    return $signature_input . "." . $jwt_signature;
}
?>

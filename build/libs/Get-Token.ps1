function Authenticate(){

    $url = "http://localhost:8081/api/v1/authentication/login"
    $body = @{

        username = ""
        password = ""

    }

    $response = Invoke-RestMethod -Method 'Post' -Uri $url -Body $body | select-string 'success' | out-string

    if ($response -match 'success'){

        #return token
        return ($response.split(";")[2].split("=")[1] -replace "`t|`n|`r","").TrimEnd()
        
    }

}

return Authenticate
sub vcl_recv {
#FASTLY recv
if (server.identity ~ "LCY$") {
      set req.http.Host = "content.guardianapis.com";
}

}

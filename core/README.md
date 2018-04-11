# kurosawa

The root Kurosawa library.

![Akira Kurosawa](http://i.imgur.com/Gydm4pM.jpg?1)


## What does it do?

Contains logging, error handling and other common utilities needed in any library or
application.

## Development

You can interact with the library in the REPL by typing in Emacs:

    M-x cider-jack-in
    user> 

Initialize the components by: 

    user> (dev)
    dev> (reset)


Try out the results namespace: 

    org.purefn.kurosawa.results> (-> (attempt / 10 2)
                                     (proceed + 3)
                                     (result))
    8
    
    org.purefn.kurosawa.results> (-> (attempt / 10 0)
                                     (proceed + 3)
                                     (failure?))
    true

## Generate Docs

    $ lein docs
    $ open doc/dist/latest/api/index.html


## Deploying 

Check-ins to the `k8s-qa` or `k8s-prod` branches will trigger Jenkins to build a JAR artifact
for the library and deploying it to Nexus on the respective clusters.


FROM denoland/deno:latest

    # Create working directory
WORKDIR /app

    # Copy source
COPY . .

    # Run the app
CMD [ "deno", "run", "--allow-net", "--allow-read", "--allow-env", "jsr:@babashka/nbb", "-x", "server/start-server" ]

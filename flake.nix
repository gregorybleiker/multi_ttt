{
  description = "Development environment with Deno and Babashka";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs = { self, nixpkgs, flake-utils }:
    flake-utils.lib.eachDefaultSystem (system:
      let
        pkgs = nixpkgs.legacyPackages.${system};
      in
      {
        # Development shell
        devShells.default = pkgs.mkShell {
          buildInputs = with pkgs; [
            deno
            babashka
            helix
            clojure
          ];

          shellHook = ''
            echo "╔════════════════════════════════════════════╗"
            echo "║  Deno + Babashka Development Environment  ║"
            echo "╚════════════════════════════════════════════╝"
            echo ""
            echo "Installed versions:"
            echo "  Deno:     $(deno --version | head -n1)"
            echo "  Babashka: $(bb --version)"
            echo ""
            echo "Quick reference:"
            echo "  Deno commands:"
            echo "    deno run <file>        - Run TypeScript/JavaScript"
            echo "    deno fmt               - Format code"
            echo "    deno lint              - Lint code"
            echo "    deno test              - Run tests"
            echo "    deno repl              - Start REPL"
            echo ""
            echo "  Babashka commands:"
            echo "    bb <file>              - Run Clojure script"
            echo "    bb -e '<expr>'         - Evaluate expression"
            echo "    bb repl                - Start REPL"
            echo "    bb tasks               - List available tasks"
            echo ""
          '';
        };

        # Package for installation
        packages.default = pkgs.buildEnv {
          name = "deno-babashka-env";
          paths = with pkgs; [
            deno
            babashka
          ];
        };
      }
    );
}

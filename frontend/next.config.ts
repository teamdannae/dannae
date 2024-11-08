// import path from "path";
import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  sassOptions: {
    silenceDeprecations: ["legacy-js-api"],
    // includePaths: [path.join(__dirname, "src/styles")],
  },
  reactStrictMode: false,
};

export default nextConfig;

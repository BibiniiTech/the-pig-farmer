import { MetadataRoute } from "next";

export default function robots(): MetadataRoute.Robots {
  return {
    rules: {
      userAgent: "*",
      allow: "/",
      disallow: ["/dashboard/", "/admin/", "/api/", "/moved"],
    },
    sitemap: "https://www.smartswine.app/sitemap.xml",
  };
}

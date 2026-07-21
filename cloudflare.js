export default {
    async fetch(request) {

        const url = new URL(request.url);

        const target = url.searchParams.get("url");

        if (!target) {
            return new Response(
                "missing url",
                { status: 400 }
            );
        }


        // 请求 B站
        const response = await fetch(target, {
            headers: {
                "User-Agent":
                    "Mozilla/5.0",

                "Referer":
                    "https://www.bilibili.com/"
            }
        });


        if (!response.ok) {
            return new Response(
                `source error: ${response.status} ${response.statusText}`
            );
        }


        // 返回下载
        return new Response(
            response.body,
            {
                headers: {

                    // 强制浏览器下载
                    "Content-Disposition":
                        'attachment; filename="video.m4a"',


                    "Content-Type":
                        "video/mp4",


                    // 支持断点续传
                    "Accept-Ranges":
                        "bytes"
                }
            }
        );
    }
};
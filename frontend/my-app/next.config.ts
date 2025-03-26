/** @type {import('next').NextConfig} */
const nextConfig = {
    async rewrites() {
        return [
            {
                source: '/api/chat/history',
                destination: 'http://localhost:8080/api/chat/history'
            }
        ];
    },
    // TypeScript 설정 추가
    typescript: {
        ignoreBuildErrors: true
    }
};

export default nextConfig;

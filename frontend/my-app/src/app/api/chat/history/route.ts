import { NextRequest, NextResponse } from 'next/server';

export async function GET(request: NextRequest) {
    const { searchParams } = new URL(request.url);
    const sender = searchParams.get('sender');
    const receiver = searchParams.get('receiver');

    try {
        // 스프링 부트 API에 프록시 요청
        const response = await fetch(`http://localhost:8080/api/chat/history?sender=${sender}&receiver=${receiver}`);

        if (!response.ok) {
            throw new Error('Failed to fetch chat history');
        }

        const data = await response.json();
        return NextResponse.json(data);
    } catch (error) {
        return NextResponse.json({ error: 'Failed to fetch chat history' }, { status: 500 });
    }
}

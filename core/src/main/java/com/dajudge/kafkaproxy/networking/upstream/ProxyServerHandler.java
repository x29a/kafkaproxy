package com.dajudge.kafkaproxy.networking.upstream;

import com.dajudge.kafkaproxy.networking.upstream.ForwardChannelFactory.UpstreamCertificateSupplier;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.ssl.SslHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import java.security.cert.Certificate;
import java.util.function.Consumer;
import java.util.function.Function;

public class ProxyServerHandler extends ChannelInboundHandlerAdapter {
    private static final Logger LOG = LoggerFactory.getLogger(ProxyServerHandler.class);
    private final Function<UpstreamCertificateSupplier, Consumer<ByteBuf>> sinkFactory;
    private Consumer<ByteBuf> sink;

    public ProxyServerHandler(final Function<UpstreamCertificateSupplier, Consumer<ByteBuf>> sinkFactory) {
        this.sinkFactory = sinkFactory;
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) {
        final ByteBuf buffer = ((ByteBuf) msg);
        LOG.trace("Received {} bytes from upstream.", buffer.readableBytes());
        try {
            sink(ctx).accept(buffer);
        } finally {
            buffer.release();
        }
    }

    private synchronized Consumer<ByteBuf> sink(final ChannelHandlerContext ctx) {
        if (sink == null) {
            final UpstreamCertificateSupplier certSupplier = () -> {
                final ChannelHandler sslHandler = ctx.channel().pipeline().get("ssl");
                if (sslHandler instanceof SslHandler) {
                    final SSLSession session = ((SslHandler) sslHandler).engine().getSession();
                    final Certificate[] clientCerts = session.getPeerCertificates();
                    return clientCerts[0];
                } else {
                    throw new SSLPeerUnverifiedException("Upstream SSL not enabled");
                }
            };
            sink = sinkFactory.apply(certSupplier);
        }
        return sink;
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) {
        LOG.debug("Exception caught in upstream channel {}", ctx.channel().remoteAddress(), cause);
        ctx.close();
    }

}